package org.twitchstats;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOption;

public class SlashCommandsSetUp
{
	public Set<SlashCommandBuilder> getCommands()
	{

		Set<SlashCommandBuilder> builders = new HashSet<>();

		builders.add(new SlashCommandBuilder().setName("twitchstats")
			.setDescription("What is going on")
			.setOptions(Arrays.asList(
				SlashCommandOption.createStringOption("channel", "twitch channel", true),
				SlashCommandOption.createStringOption("start", "start", true),
				SlashCommandOption.createStringOption("end", "end", true)
			))
		);

		return builders;
	}
}
