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

public class TwitchStatsEmbedWorker
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

				String channelid = "austingambles";
				double start = 0;
				double end = 0;
				String units = "";
				for (SlashCommandInteractionOption interactionOption : arg)
				{
					if (interactionOption.getName().equals("start"))
					{
						start = interactionOption.getDecimalValue().get();
					}
					if (interactionOption.getName().equals("end"))
					{
						end = interactionOption.getDecimalValue().get();
					}
					if (interactionOption.getName().equals("units"))
					{
						units = interactionOption.getStringValue().get();
					}
				}

				EmbedBuilder embedBuilder = new EmbedBuilder();
				embedBuilder.setTitle("Twitch Stats - " + channelid);
				embedBuilder.setAuthor(api.getYourself());


				Instant now = Instant.now();

				long DAY = 24;
				long multiplier = 60 * 60 * (units.equals("h") ? 1 : units.equals("d") ? DAY : 0);
				long startSeconds = (long) start * multiplier;
				long endSeconds = (long) end * multiplier;

				if (endSeconds > startSeconds)
				{
					long temp = startSeconds;
					startSeconds = endSeconds;
					endSeconds = temp;
				}

				Instant start1 = now.minus(startSeconds, ChronoUnit.SECONDS);
				Instant end1 = now.minus(endSeconds, ChronoUnit.SECONDS);

				int peakViewersTotal = 0;
				int peakViewersPeriod = 0;
				int totalBans = 0;
				int periodBans = 0;
				int totalTimeouts = 0;
				int periodTimeouts = 0;
				int totalStreams = 0;
				int periodStreams = 0;
				int sumPeriodMinutes = 0;
				int sumTotalMinutes = 0;

				int longestStream = 0;
				int shortestStream = 99999999;

				int sumPeriodMinutesEver = 0;
				int longestStreamEver = 0;
				int shortestStreamEver = 99999999;
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
					totalTimeouts+= Integer.parseInt(streamStat.getTotalTimeouts());
					totalStreams++;
					if (streamStat.highestViewerCount != null && !streamStat.highestViewerCount.isEmpty())
					{
						if (peakViewersTotal < Integer.parseInt(streamStat.highestViewerCount))
						{
							peakViewersTotal = Integer.parseInt(streamStat.highestViewerCount);
						}
					}

					// these are session stats
					String s1 = streamStat.streamStartTime;
					String s2 = streamStat.streamEndTime;
					Instant streamEnd;

					if (s2 == null || s2.isEmpty())
					{
						if (endSeconds == 0)
						{
							streamEnd = Instant.now();
						}
						else
						{
							continue;
						}
					}
					else
					{
						streamEnd = Instant.parse(s2);
					}

					Instant streamStart = Instant.parse(s1);
					int minutes = (int) ChronoUnit.MINUTES.between(streamStart, streamEnd);
					sumTotalMinutes += minutes;
					if (minutes > longestStreamEver)
					{
						longestStreamEver = minutes;
					}
					if (minutes < shortestStreamEver)
					{
						shortestStreamEver = minutes;
					}
					if (!start1.isBefore(streamStart))
						continue;
					if (!end1.isAfter(streamEnd))
						continue;

					if (streamStat.highestViewerCount != null && !streamStat.highestViewerCount.isEmpty())
					{
						if (peakViewersPeriod < Integer.parseInt(streamStat.highestViewerCount))
						{
							peakViewersPeriod = Integer.parseInt(streamStat.highestViewerCount);
						}
					}

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
					periodTimeouts += Integer.parseInt(streamStat.getTotalTimeouts());
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
					embedBuilder.addInlineField("Period Streams", String.valueOf(periodStreams));

					if (periodStreams != 0)
					{
						embedBuilder.addInlineField("Longest Stream", longestStream + " minutes");
						embedBuilder.addInlineField("Shortest Stream", shortestStream + " minutes");
					}
					else
					{
						embedBuilder.addInlineField("Longest Stream", "No streams in period");
						embedBuilder.addInlineField("Shortest Stream", "No streams in period");
					}
					embedBuilder.addInlineField("Bans in period", String.valueOf(periodBans));
					embedBuilder.addInlineField("Timeouts in period", String.valueOf(periodTimeouts));
					embedBuilder.addInlineField("Period Unique Chatters", String.valueOf(uniqueChattersPeriod.size()));
					embedBuilder.addInlineField("Period Peak Viewers", String.valueOf(peakViewersPeriod));

					if (periodStreams != 0)
					{
						embedBuilder.addInlineField("Average Stream Length Period", String.valueOf((int) sumPeriodMinutes / periodStreams) + " minutes");
					}
					else
					{
						embedBuilder.addInlineField("Average Stream Length Period", "No streams in period");
					}
					embedBuilder.addInlineField("", "");
					embedBuilder.addInlineField("Total Streams", String.valueOf(totalStreams));
					if (totalStreams != 0)
					{
						embedBuilder.addInlineField("Longest Ever Stream", longestStreamEver + " minutes");
						embedBuilder.addInlineField("Shortest Ever Stream", shortestStreamEver + " minutes");
					}
					else
					{
						embedBuilder.addInlineField("Longest Stream", "No streams ever");
						embedBuilder.addInlineField("Shortest Stream", "No streams ever");

					}
					embedBuilder.addInlineField("Total Bans", String.valueOf(totalBans));
					embedBuilder.addInlineField("Total Timeouts", String.valueOf(totalTimeouts));
					embedBuilder.addInlineField("Total Unique Chatters", String.valueOf(uniqueChattersTotal.size()));
					embedBuilder.addInlineField("Peak Viewers", String.valueOf(peakViewersTotal));
					if (totalStreams != 0)
					{
						embedBuilder.addInlineField("Average Stream Length", String.valueOf((int) sumTotalMinutes / totalStreams) + " minutes");
					}
					else
					{
						embedBuilder.addInlineField("Average Stream Length", "No streams ever");
					}
					embedBuilder.addInlineField("", "");

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
