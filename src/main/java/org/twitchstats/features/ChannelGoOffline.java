package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import java.time.Instant;
import org.twitchstats.Main;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;
import org.twitchstats.workers.TwitchStatsWorker;

public class ChannelGoOffline
{
	public ChannelGoOffline(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelGoOfflineEvent.class, event -> onChannelMessage(event));
	}

	/**
	 * Subscribe to the ChannelMessage Event and write the output to the console
	 */
	public void onChannelMessage(ChannelGoOfflineEvent event) {
		String endTime = Instant.now().toString();
		System.out.println(String.format(
			"Channel [%s] - Ended at [%s]",
			event.getChannel().getName(), endTime)
		);

		String channelID = event.getChannel().getName();

		if (!CURRENT_STREAM.getOrDefault(event.getChannel().getName(), "").isEmpty())
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName());
			StreamStat streamStat = STREAM_STATS.get(streamID + channelID);

			if (streamStat != null)
			{
				streamStat.streamEndTime = Instant.now().toString();
				TwitchStatsWorker worker = new TwitchStatsWorker();
				worker.execute(Main.discordApi).join();
//				CURRENT_STREAM.put(event.getChannel().getName(), "");
			}
		}
	}
}
