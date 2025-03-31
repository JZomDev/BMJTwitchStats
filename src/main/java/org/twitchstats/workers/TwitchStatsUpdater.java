package org.twitchstats.workers;

import com.github.twitch4j.helix.domain.Stream;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.javacord.api.DiscordApi;
import org.twitchstats.Bot;
import org.twitchstats.Main;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.DIR;
import org.twitchstats.StreamStat;

public class TwitchStatsUpdater
{
	private Bot bot;

	private static final Type typeToken = new TypeToken<List<StreamStat>>()
	{
	}.getType();

	public CompletableFuture<Void> execute(DiscordApi api)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				Gson gson = new Gson();
				String fileName = DIR + File.separator + "twitchstats.json";
				String serverText = Files.readString(Paths.get(fileName));

				List<StreamStat> json = gson.fromJson(serverText, typeToken);

				for (String channelid: CURRENT_STREAM.keySet())
				{
					Stream currentStream = CURRENT_STREAM.getOrDefault(channelid, null);
					if (currentStream == null) continue;

					StreamStat streamStat = Main.STREAM_STATS.get(currentStream.getId() + channelid);
					if (streamStat == null || json == null)
					{
						return null;
					}
					boolean edited = false;
					for (int i = 0; i < json.size(); i++)
					{
						StreamStat s = json.get(i);
						if (streamStat.streamID.equals(s.streamID) && streamStat.channel.equals(s.channel))
						{
							json.set(i, streamStat);
							edited = true;
						}
					}
					if (!edited)
					{
						json.add(streamStat);
					}

					BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(fileName, false));
					bufferedWriter.write(gson.toJson(json));
					bufferedWriter.newLine();
					bufferedWriter.flush();
					bufferedWriter.close();
				}
			}
			catch (Exception e)
			{
				throw new RuntimeException(e.getMessage(), e);
			}
			return null;
		}, api.getThreadPool().getExecutorService());
	}
}
