package org.twitchstats;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionChoice;
import org.javacord.api.interaction.SlashCommandOptionType;

public class SlashCommandsSetUp
{
	public Set<SlashCommandBuilder> getCommands()
	{

		Set<SlashCommandBuilder> builders = new HashSet<>();

		builders.add(new SlashCommandBuilder().setName("twitchstats")
			.setDescription("Gets BMJ twitch stats")
			.setOptions(Arrays.asList(
				SlashCommandOption.createDecimalOption("start", "start", true),
				SlashCommandOption.createDecimalOption("end", "end", true),
				SlashCommandOption.createWithChoices(SlashCommandOptionType.STRING, "units", "Unit of time", true,
					Arrays.asList(
						SlashCommandOptionChoice.create("hours", "h"),
						SlashCommandOptionChoice.create("days", "d")))
			))
		);

		return builders;
	}
}
