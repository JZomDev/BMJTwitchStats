package org.twitchstats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.ITwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.twitchstats.Main.DIR;
import org.twitchstats.features.ChannelGoLive;
import org.twitchstats.features.ChannelGoOffline;
import org.twitchstats.features.ChannelMessage;
import org.twitchstats.features.ChannelViewerCountUpdate;
import org.twitchstats.features.UserBan;


public class Bot {

	/**
	 * Holds the Bot Configuration
	 */
	private Configuration configuration;

	/**
	 * Twitch4J API
	 */
	private ITwitchClient twitchClient;

	/**
	 * Constructor
	 */
	public Bot() {
		// Load Configuration
		loadConfiguration();

		TwitchClientBuilder clientBuilder = TwitchClientBuilder.builder();

		//region Auth
		OAuth2Credential credential = new OAuth2Credential(
			"twitch",
			configuration.getCredentials().get("irc")
		);
		//endregion

		//region TwitchClient
		twitchClient = clientBuilder
			.withClientId(configuration.getApi().get("twitch_client_id"))
			.withClientSecret(configuration.getApi().get("twitch_client_secret"))
			.withEnableHelix(true)
			/*
			 * Chat Module
			 * Joins irc and triggers all chat based events (viewer join/leave/sub/bits/gifted subs/...)
			 */
			.withChatAccount(credential)
			.withEnableChat(true)
			/*
			 * Build the TwitchClient Instance
			 */
			.build();
		//endregion
	}

	/**
	 * Method to register all features
	 */
	public void registerFeatures() {
		SimpleEventHandler eventHandler = twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class);

		// Register Event-based features
		UserBan banChannelChatToConsole = new UserBan(eventHandler);
		ChannelGoOffline channelGoOffline = new ChannelGoOffline(eventHandler);
		ChannelGoLive channelGoLive = new ChannelGoLive(eventHandler);
		ChannelViewerCountUpdate channelViewerCountUpdate = new ChannelViewerCountUpdate(eventHandler);
		ChannelMessage channelMessage = new ChannelMessage(eventHandler);
	}

	/**
	 * Load the Configuration
	 */
	private void loadConfiguration() {
		try {
			String fileName = DIR + File.separator + "config.yaml";
			String text = Files.readString(Paths.get(fileName));
			ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
			configuration = mapper.readValue(text, Configuration.class);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Unable to load Configuration ... Exiting.");
			System.exit(1);
		}
	}

	public void start() {
		// Connect to all channels
		for (String channel : configuration.getChannels()) {
			twitchClient.getChat().joinChannel(channel);
		}

		// Enable client helper for Stream GoLive / GoOffline / GameChange / TitleChange Events
		twitchClient.getClientHelper().enableStreamEventListener(configuration.getChannels());
		// Enable client helper for Follow Event
		twitchClient.getClientHelper().enableFollowEventListener(configuration.getChannels());
	}

	public ITwitchClient getTwitchClient() {
		return twitchClient;
	}
}