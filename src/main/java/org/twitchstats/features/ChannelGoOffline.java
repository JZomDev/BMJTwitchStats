package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.twitchstats.Main;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;
import org.twitchstats.workers.TwitchStatsUpdater;

public class ChannelGoOffline
{
	private static final Logger logger = LogManager.getLogger(ChannelGoOffline.class);

	public ChannelGoOffline(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelGoOfflineEvent.class, event -> onGoOffline(event));
	}

	public void onGoOffline(ChannelGoOfflineEvent event) {
		String endTime = Instant.now().toString();
		logger.info(String.format(
			"Channel [%s] - Ended at [%s]",
			event.getChannel().getName(), endTime)
		);

		String channelID = event.getChannel().getName();

		if (CURRENT_STREAM.getOrDefault(event.getChannel().getName(), null) != null)
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName()).getId();
			StreamStat streamStat = STREAM_STATS.get(streamID + channelID);

			if (streamStat != null)
			{
				streamStat.streamEndTime = Instant.now().toString();
				TwitchStatsUpdater worker = new TwitchStatsUpdater();
				worker.execute(Main.discordApi).join();
			}
		}
	}
}
