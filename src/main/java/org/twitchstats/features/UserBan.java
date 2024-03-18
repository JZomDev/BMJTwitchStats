package org.twitchstats.features;

import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.chat.events.channel.UserBanEvent;
import static org.twitchstats.Main.CURRENT_STREAM;
import static org.twitchstats.Main.STREAM_STATS;
import org.twitchstats.StreamStat;

public class UserBan
{
	public UserBan(SimpleEventHandler eventHandler) {
		eventHandler.onEvent(UserBanEvent.class, event -> onChannelMessage(event));
	}

	/**
	 * Subscribe to the ChannelMessage Event and write the output to the console
	 */
	public void onChannelMessage(UserBanEvent event) {
		System.out.println(String.format(
			"Ban Channel [%s] - Banned User[%s]",
			event.getChannel().getName(),
			event.getUser().getName())
		);

		if (!CURRENT_STREAM.getOrDefault(event.getChannel().getName(), "").isEmpty())
		{
			String streamID = CURRENT_STREAM.get(event.getChannel().getName());
			String channelID = event.getChannel().getName();
			StreamStat streamStat = STREAM_STATS.get(streamID + channelID);
			if (streamStat != null) {
				int bans = Integer.parseInt(streamStat.totalBans) + 1;
				streamStat.totalBans = String.valueOf(bans);
				STREAM_STATS.put(streamID + channelID, streamStat);
			}
		}
	}
}
