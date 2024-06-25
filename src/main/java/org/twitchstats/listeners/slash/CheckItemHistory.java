package org.twitchstats.listeners.slash;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.twitchstats.workers.TwitchStatsEmbedWorker;

public class CheckItemHistory implements SlashCommandCreateListener
{


	public CheckItemHistory()
	{

	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event)
	{
		SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();

		DiscordApi api = event.getApi();
		TwitchStatsEmbedWorker twitchStatsEmbed = new TwitchStatsEmbedWorker();

		if (slashCommandInteraction.getCommandName().equals("twitchstats"))
		{
			slashCommandInteraction.respondLater(false)
				.thenAccept(interactionOriginalResponseUpdater -> {
					interactionOriginalResponseUpdater
						.setFlags(MessageFlag.LOADING)
						.update();
					try
					{
						CompletableFuture<EmbedBuilder> futureTwitchStats = twitchStatsEmbed.execute(api, slashCommandInteraction.getArguments());
						EmbedBuilder embedBuilder = futureTwitchStats.orTimeout(10, TimeUnit.SECONDS).get();

						if (embedBuilder != null)
						{
							slashCommandInteraction.createFollowupMessageBuilder()
								.addEmbed(embedBuilder)
								.send();
						} else {
							slashCommandInteraction.createFollowupMessageBuilder()
								.addEmbed(embedBuilder)
								.send();
						}

					}
					catch (Exception e)
					{
						System.out.println(e.getMessage());
						e.printStackTrace();

						interactionOriginalResponseUpdater
							.delete()
							.join();

						slashCommandInteraction.createFollowupMessageBuilder()
							.setContent("Failed to get data")
							.setFlags(MessageFlag.EPHEMERAL)
							.send()
							.join();
					}
				});
		}
	}
}
