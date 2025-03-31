package org.twitchstats;

import com.github.twitch4j.helix.domain.Stream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.twitchstats.listeners.serveravailable.ServerBecomesAvailable;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.twitchstats.listeners.slash.CheckItemHistory;
import org.twitchstats.workers.TwitchStatsUpdater;

public class Main
{

	private static final Logger logger = LogManager.getLogger(Main.class);

	public static String DISCORD_TOKEN = "";
	public static String DIR = "";
	private static ScheduledExecutorService mService;
	public static DiscordApi discordApi;

	// streamid_channelid, StreamStat
	public static HashMap<String, StreamStat> STREAM_STATS = new HashMap<>();
	// channelid, streamid
	public static HashMap<String, Stream> CURRENT_STREAM = new HashMap<>();

	static
	{
		Map<String, String> environmentVariables = System.getenv();

		for (String envName : environmentVariables.keySet())
		{
			if (envName.equals("config_dir"))
			{
				DIR = environmentVariables.get(envName) + File.separator;
			}
			if (envName.equals("bot_token"))
			{
				DISCORD_TOKEN = environmentVariables.get(envName);
			}
		}
	}

	/**
	 * The entrance point of our program.
	 *
	 * @param args The arguments for the program. The first element should be the bot's token.
	 */
	public static void main(String[] args) throws Exception
	{
		if (DISCORD_TOKEN.equals(""))
		{
			logger.error("Failed to start Discord bot. No Discord token supplied");
		}
		else
		{
			DiscordApiBuilder builder = new DiscordApiBuilder();
			builder.setAllIntents();
			builder.setToken(DISCORD_TOKEN);
			builder.setTrustAllCertificates(false);
			builder.setWaitForServersOnStartup(true);
			builder.setWaitForUsersOnStartup(false);
			builder.addServerBecomesAvailableListener(new ServerBecomesAvailable());
			builder.addSlashCommandCreateListener(new CheckItemHistory());
			discordApi = builder.login().join();
			SlashCommandsSetUp slashCommandsSetUp = new SlashCommandsSetUp();
			discordApi.bulkOverwriteGlobalApplicationCommands(slashCommandsSetUp.getCommands()).join();

			logger.info("Invite link is: {}", discordApi.createBotInvite());
			logger.info("Bot's name is {}", discordApi.getYourself().getName());

			Bot bot = new Bot();
			logger.info("Bot created");
			bot.registerFeatures();
			logger.info("registered");
			bot.start();
			logger.info("started");

			TwitchStatsUpdater worker = new TwitchStatsUpdater();

			launchScheduledExecutor(worker);
		}
	}

	public static void launchScheduledExecutor(TwitchStatsUpdater worker)
	{
		if (mService == null || mService.isShutdown())
		{
			mService = Executors.newScheduledThreadPool(1);

		}
		mService.scheduleAtFixedRate(() -> {
				// Perform your recurring method calls in here.
				try
				{
					logger.info("Updating file");
					worker.execute(discordApi);
				}
				catch (Exception e)
				{
					// don't stop process, just log the error and try again
					logger.error(e.getMessage(), e);
				}
			},
			0, // How long to delay the start
			30, // How long between executions
			TimeUnit.SECONDS); // The time unit used
	}
}