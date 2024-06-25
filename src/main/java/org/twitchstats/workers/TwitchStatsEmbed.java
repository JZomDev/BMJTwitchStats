package org.twitchstats.workers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import static org.twitchstats.Main.DIR;
import org.twitchstats.StreamStat;

public class TwitchStatsEmbed
{
	private static final Type typeToken = new TypeToken<List<StreamStat>>()
	{
	}.getType();

	public CompletableFuture<EmbedBuilder> execute(DiscordApi api, List<SlashCommandInteractionOption> arg)
	{
		return CompletableFuture.supplyAsync(() -> {
			try
			{
				Gson gson = new Gson();
				String fileName = DIR + File.separator + "twitchstats.json";
				String serverText = Files.readString(Paths.get(fileName));

				List<StreamStat> json = gson.fromJson(serverText, typeToken);

				String channelid = "thebossmanjack";
				String start = "";
				String end = "";
				for (SlashCommandInteractionOption interactionOption : arg)
				{
					if (interactionOption.getName().equals("start"))
					{
						start = interactionOption.getStringValue().get();
					}
					if (interactionOption.getName().equals("end"))
					{
						end = interactionOption.getStringValue().get();
					}
				}

				EmbedBuilder embedBuilder = new EmbedBuilder();
				embedBuilder.setTitle("Twitch Stats - " + channelid);
				embedBuilder.setAuthor(api.getYourself());

				HashMap<String, String> validEndings = new HashMap<>();
				validEndings.put("hrs", "h");
				validEndings.put("hr", "h");
				validEndings.put("h", "h");
				validEndings.put("d", "d");
				validEndings.put("day", "d");
				validEndings.put("days", "d");
				validEndings.put("week", "w");
				validEndings.put("w", "w");
				validEndings.put("weeks", "w");
				validEndings.put("wks", "w");
				String numberStart = "";
				String numberEnd = "";
				String unitStart = "";
				String unitEnd = "";
				for (String validEnding : validEndings.keySet())
				{
					if (start.toLowerCase().endsWith(validEnding))
					{
						numberStart = start.substring(0, start.length() - validEnding.length());
						unitStart = validEndings.get(validEnding);
					}
				}

				for (String validEnding : validEndings.keySet())
				{
					if (end.toLowerCase().endsWith(validEnding))
					{
						numberEnd = end.substring(0, end.length() - validEnding.length());
						unitEnd = validEndings.get(validEnding);
					}
				}

				Instant now = Instant.now();

				ChronoUnit startUnits = unitStart.equals("h") ? ChronoUnit.HOURS :
					unitStart.equals("d") ? ChronoUnit.DAYS :
						unitStart.equals("w") ? ChronoUnit.WEEKS : null;

				if (startUnits == null)
				{
					embedBuilder.addField("Bad units used", "womp womp");
				}

				ChronoUnit endUnits = unitEnd.equals("h") ? ChronoUnit.HOURS :
					unitEnd.equals("d") ? ChronoUnit.DAYS :
						unitEnd.equals("w") ? ChronoUnit.WEEKS : null;

				if (endUnits == null)
				{
					embedBuilder.addField("Bad units used", "womp womp");
				}
				Instant start1 = now.minus(Integer.parseInt(numberStart), startUnits);
				Instant end1 = now.minus(Integer.parseInt(numberEnd), endUnits);

				if (start1.isAfter(end1))
				{
					Instant temp = start1;
					start1 = end1;
					end1 = temp;
				}

				int peakViewers = 0;
				int totalBans = 0;
				int periodBans = 0;
				int totalStreams = 0;
				int periodStreams = 0;
				int longestStream = 0;
				int sumPeriodMinutes = 0;
				int sumTotalMinutes = 0;
				int shortestStream = 99999999;
				ArrayList<String> uniqueChattersPeriod = new ArrayList<>();
				ArrayList<String> uniqueChattersTotal = new ArrayList<>();

				boolean neverRan = true;
				for (StreamStat streamStat : json)
				{
					String chan = streamStat.channel;
					if (!chan.equalsIgnoreCase(channelid))
						continue;
					neverRan = false;
					uniqueChattersTotal.addAll(new ArrayList<>(Arrays.asList(streamStat.uniqueChatters)));
					totalBans+= Integer.parseInt(streamStat.totalBans);
					totalStreams++;
					if (peakViewers < Integer.parseInt(streamStat.highestViewerCount))
					{
						peakViewers = Integer.parseInt(streamStat.highestViewerCount);
					}

					// these are session stats

					String s1 = streamStat.streamStartTime;
					String s2 = streamStat.streamEndTime;
					if (s2 == null || s2.isEmpty())
						continue;
					Instant streamStart = Instant.parse(s1);
					Instant streamEnd = Instant.parse(s2);
					int minutes = (int) ChronoUnit.MINUTES.between(streamStart, streamEnd);
					sumTotalMinutes += minutes;
					if (!start1.isBefore(streamStart))
						continue;
					if (!end1.isAfter(streamEnd))
						continue;

					sumPeriodMinutes += minutes;
					if (minutes > longestStream)
					{
						longestStream = minutes;
					}
					if (minutes < shortestStream)
					{
						shortestStream = minutes;
					}

					periodStreams++;
					uniqueChattersPeriod.addAll(new ArrayList<>(Arrays.asList(streamStat.uniqueChatters)));

					periodBans += Integer.parseInt(streamStat.totalBans);
				}

				if (neverRan)
				{
					embedBuilder.addField("No Stats in for this channel", "womp womp");
				}
				else
				{
					Date date1 = new Date(start1.toEpochMilli());
					Date date2 = new Date(end1.toEpochMilli());
					embedBuilder.addField("Period", "from " + date1 + " to " + date2);
					embedBuilder.addInlineField("Total Streams", String.valueOf(totalStreams));
					embedBuilder.addInlineField("Average Stream Length", String.valueOf((int) sumTotalMinutes / totalStreams) + " minutes");
					embedBuilder.addInlineField("Total Bans", String.valueOf(totalBans));
					embedBuilder.addInlineField("Peak Viewers", String.valueOf(peakViewers));

					embedBuilder.addInlineField("Period Streams", String.valueOf(periodStreams));
					embedBuilder.addInlineField("Longest Streams", String.valueOf(longestStream) + " minutes");
					embedBuilder.addInlineField("Shortest Streams", String.valueOf(shortestStream) + " minutes");
					embedBuilder.addInlineField("Average Stream Length Period", String.valueOf((int) sumPeriodMinutes / periodStreams) + " minutes");
					embedBuilder.addInlineField("Bans in period", String.valueOf(periodBans));
					embedBuilder.addInlineField("Total Unique Chatters", String.valueOf(uniqueChattersTotal.size()));
					embedBuilder.addInlineField("Period Unique Chatters", String.valueOf(uniqueChattersPeriod.size()));
				}

				return embedBuilder;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e.getMessage(), e);
			}
		}, api.getThreadPool().getExecutorService());
	}
}
