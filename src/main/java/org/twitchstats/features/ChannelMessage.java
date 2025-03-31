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
		if (CURRENT_STREAM.containsKey(event.getChannel().getName()) && CURRENT_STREAM.get(event.getChannel().getName()) != null)
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName()).getId();
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
				logger.info("Message Channel [{}] - User[{}] - Message[{}] - Time[{}]", event.getChannel().getName(), event.getUser().getName(), event.getMessage(), getTimeFromSeconds(CURRENT_STREAM.get(event.getChannel().getName()).getUptime().getSeconds()));
			}
			else
			{
				logger.info("Message Channel [{}] - User[{}] - Message[{}]", event.getChannel().getName(), event.getUser().getName(), event.getMessage());
			}
		}
		else
		{
			logger.info("Message Channel [{}] - User[{}] - Message[{}]", event.getChannel().getName(), event.getUser().getName(), event.getMessage());
		}
	}

	private String getTimeFromSeconds(long seconds) {
		return getTimeFromSeconds(String.valueOf(seconds));
	}

	private String getTimeFromSeconds(String seconds)
	{
		long secondsLong = Long.parseLong(seconds);
		int hours = (int) (secondsLong / 3600);
		int minutes = (int) ((secondsLong - (hours * 3600)) / 60);
		int seconds2 = (int) (secondsLong - (hours * 3600) - (minutes * 60));
		return hours + ":" + (minutes < 10 ? "0" + minutes : minutes) + ":" + (seconds2 < 10 ? "0" + seconds2 : seconds2);
	}
}
