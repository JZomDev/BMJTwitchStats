package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.helix.domain.Stream;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;

public class ChannelGoLive
{
	public ChannelGoLive(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelGoLiveEvent.class, event -> onChannelMessage(event));
	}

	/**
	 * Subscribe to the ChannelMessage Event and write the output to the console
	 */
	public void onChannelMessage(ChannelGoLiveEvent event) {
		Stream stream = event.getStream();
		String startTime = stream.getStartedAtInstant().toString();
		System.out.println(String.format(
			"StreamID [%s] - Channel [%s] - Started at [%s]",
			event.getStream().getId(),
			event.getChannel().getName(),
			startTime)
		);
		String streamID = event.getStream().getId();
		String channelID = event.getChannel().getName();

		CURRENT_STREAM.put(channelID, streamID);
		StreamStat streamStat = StreamStat.getStreamStat(event.getStream());
		STREAM_STATS.putIfAbsent(streamID + channelID, streamStat);
	}
}
