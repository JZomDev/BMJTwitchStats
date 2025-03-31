package org.twitchstats;

import com.github.twitch4j.helix.domain.Stream;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import static org.twitchstats.Main.DIR;

public class StreamStat
{
	private static final Type typeToken = new TypeToken<List<StreamStat>>()
	{
	}.getType();

	@SerializedName("StreamID")
	public String streamID;

	@SerializedName("Channel")
	public String channel;

	@SerializedName("highestViewerCount")
	public String highestViewerCount;

	@SerializedName("streamStartTime")
	public String streamStartTime;

	@SerializedName("streamEndTime")
	public String streamEndTime;

	@SerializedName("uniqueChatters")
	public String[] uniqueChatters;

	@SerializedName("totalBans")
	public String totalBans;

	@SerializedName("totalTimeouts")
	public String totalTimeouts;

	public static StreamStat getStreamStat(Stream stream)
	{
		String streamID = stream.getId();
		try
		{
			Gson gson = new Gson();
			String fileName = DIR + File.separator + "twitchstats.json";
			String serverText = Files.readString(Paths.get(fileName));

			List<StreamStat> json = gson.fromJson(serverText, typeToken);

			if (!json.isEmpty())
			{
				for (StreamStat streamStat : json)
				{
					if (streamStat.streamID.equals(streamID))
					{
						return streamStat;
					}
				}
			}
		}
		catch (Exception e)
		{
			// do nothing
		}

		StreamStat streamStat = new StreamStat();
		streamStat.streamID = streamID;
		streamStat.channel = stream.getUserName();
		streamStat.streamStartTime = stream.getStartedAtInstant().toString();
		streamStat.totalBans = "0";
		streamStat.totalTimeouts = "0";
		streamStat.uniqueChatters = new String[]{};

		return streamStat;
	}

	public String getTotalTimeouts()
	{
		if (totalTimeouts == null)
		{
			return "0";
		}
		return totalTimeouts;
	}
}
