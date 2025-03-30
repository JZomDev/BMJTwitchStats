package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import java.util.ArrayList;
import java.util.Arrays;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.twitchstats.StreamStat;

public class ChannelMessage
{
	private static final Logger logger = LogManager.getLogger(ChannelMessage.class);

	public ChannelMessage(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(ChannelMessageEvent.class, event -> onChannelMessage(event));
	}

	/**
	 * Subscribe to the ChannelMessage Event and write the output to the console
	 */
	public void onChannelMessage(ChannelMessageEvent event) {
		logger.info("Message Channel [{}] - User[{}] - Message[{}]", event.getChannel().getName(), event.getUser().getName(), event.getMessage());

		if (!CURRENT_STREAM.getOrDefault(event.getChannel().getName(), "").isEmpty())
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName());
			String channelID = event.getChannel().getName();

			StreamStat streamStat = STREAM_STATS.get(streamID + channelID);
			if (streamStat != null) {
				ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(streamStat.uniqueChatters));
				if (!arrayList.contains(event.getUser().getName()))
				{
					arrayList.add(event.getUser().getName());
				}
				streamStat.uniqueChatters = arrayList.toArray(new String[0]);
				STREAM_STATS.put(streamID + channelID, streamStat);
			}
		}
	}
}
