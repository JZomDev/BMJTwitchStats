package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.helix.domain.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;

public class ChannelGoLive
{
	private static final Logger logger = LogManager.getLogger(ChannelGoLive.class);

	public ChannelGoLive(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelGoLiveEvent.class, event -> onGoLive(event));
	}

	public void onGoLive(ChannelGoLiveEvent event) {
		Stream stream = event.getStream();
		String startTime = stream.getStartedAtInstant().toString();
		logger.info(String.format(
			"StreamID [%s] - Channel [%s] - Started at [%s]",
			event.getStream().getId(),
			event.getChannel().getName(),
			startTime)
		);
		String streamID = event.getStream().getId();
		String channelID = event.getChannel().getName();

		CURRENT_STREAM.put(channelID, event.getStream());
		StreamStat streamStat = StreamStat.getStreamStat(event.getStream());
		STREAM_STATS.put(streamID + channelID, streamStat);
	}
}
