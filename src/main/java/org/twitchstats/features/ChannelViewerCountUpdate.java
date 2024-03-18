package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.events.ChannelViewerCountUpdateEvent;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;

public class ChannelViewerCountUpdate
{
	public ChannelViewerCountUpdate(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelViewerCountUpdateEvent.class, event -> onChannelMessage(event));
	}

	/**
	 * Subscribe to the ChannelMessage Event and write the output to the console
	 */
	public void onChannelMessage(ChannelViewerCountUpdateEvent event) {
		System.out.println(String.format(
			"ViewerCount StreamID [%s] - Channel [%s] - Viewer Count[%s] - start time[%s]",
			event.getStream().getId(),
			event.getChannel().getName(),
			event.getStream().getViewerCount(),
			event.getStream().getStartedAtInstant()
			)
		);

		if (CURRENT_STREAM.getOrDefault(event.getChannel().getName(), "").isEmpty())
		{
			String streamID = event.getStream().getId();
			String channelID = event.getChannel().getName();

			CURRENT_STREAM.put(channelID, streamID);
			StreamStat streamStat2 = StreamStat.getStreamStat(event.getStream());

			STREAM_STATS.putIfAbsent(streamID + channelID, streamStat2);
		}

		if (!CURRENT_STREAM.getOrDefault(event.getChannel().getName(), "").isEmpty())
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName());
			String channelID = event.getChannel().getName();
			StreamStat streamStat = STREAM_STATS.get(streamID + channelID);
			if (streamStat != null)
			{
				String viewerCount = streamStat.highestViewerCount;
				if (viewerCount == null)
				{
					streamStat.highestViewerCount = String.valueOf(event.getStream().getViewerCount());
				}
				else if (event.getStream().getViewerCount() > Integer.parseInt(streamStat.highestViewerCount))
				{
					streamStat.highestViewerCount = String.valueOf(event.getStream().getViewerCount());
				}
				STREAM_STATS.put(streamID + channelID, streamStat);
			}
		}
	}
}
