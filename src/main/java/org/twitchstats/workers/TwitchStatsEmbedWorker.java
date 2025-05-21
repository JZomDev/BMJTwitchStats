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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.twitchstats.Main;
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

				ArrayList<String> channelid = new ArrayList<>(Arrays.asList(Main.CHANNEL_NAMES.split("-")));
				double start = 0;
				double end = 0;
				String units = "";
				for (SlashCommandInteractionOption interactionOption : arg)
				{
					if (interactionOption.getName().equals("start") && interactionOption.getDecimalValue().isPresent())
					{
						start = interactionOption.getDecimalValue().get();
					}
					if (interactionOption.getName().equals("end") && interactionOption.getDecimalValue().isPresent())
					{
						end = interactionOption.getDecimalValue().get();
					}
					if (interactionOption.getName().equals("units") && interactionOption.getStringValue().isPresent())
					{
						units = interactionOption.getStringValue().get();
					}
				}

				EmbedBuilder embedBuilder = new EmbedBuilder();
				embedBuilder.setTitle("Twitch Stats - " + channelid.get(0));
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
				int sumPeriodSeconds = 0;
				int sumTotalSeconds = 0;

				int longestStream = 0;
				int shortestStream = 99999999;

				int longestStreamEver = 0;
				int shortestStreamEver = 99999999;
				ArrayList<String> uniqueChattersPeriod = new ArrayList<>();
				ArrayList<String> uniqueChattersTotal = new ArrayList<>();

				boolean neverRan = true;
				for (StreamStat streamStat : json)
				{
					String chan = streamStat.channel;
					if (!channelid.contains(chan))
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
					String streamStartTime = streamStat.streamStartTime;
					String streamEndTime = streamStat.streamEndTime;
					Instant streamEnd;

					if (streamEndTime == null || streamEndTime.isEmpty())
					{
						if (endSeconds == 0)
						{
							streamEnd = Instant.now().plus(-1, ChronoUnit.SECONDS);
						}
						else
						{
							continue;
						}
					}
					else
					{
						streamEnd = Instant.parse(streamEndTime);
					}

					Instant streamStart = Instant.parse(streamStartTime);
					int seconds = (int) ChronoUnit.SECONDS.between(streamStart, streamEnd);
					sumTotalSeconds += seconds;
					if (seconds > longestStreamEver)
					{
						longestStreamEver = seconds;
					}
					if (seconds < shortestStreamEver)
					{
						shortestStreamEver = seconds;
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

					sumPeriodSeconds += seconds;
					if (seconds > longestStream)
					{
						longestStream = seconds;
					}
					if (seconds < shortestStream)
					{
						shortestStream = seconds;
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
						embedBuilder.addInlineField("Longest Stream", getTimeFromSeconds(longestStream));
						embedBuilder.addInlineField("Shortest Stream", getTimeFromSeconds(shortestStream));
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
						embedBuilder.addInlineField("Average Stream Length Period", getTimeFromSeconds(sumPeriodSeconds / periodStreams));
					}
					else
					{
						embedBuilder.addInlineField("Average Stream Length Period", "No streams in period");
					}
					embedBuilder.addInlineField("", "");
					embedBuilder.addInlineField("Total Streams", String.valueOf(totalStreams));
					if (totalStreams != 0)
					{
						embedBuilder.addInlineField("Longest Ever Stream", getTimeFromSeconds(longestStreamEver));
						embedBuilder.addInlineField("Shortest Ever Stream", getTimeFromSeconds(shortestStreamEver));
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
						embedBuilder.addInlineField("Average Stream Length", getTimeFromSeconds(sumTotalSeconds / totalStreams));
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

	private String getTimeFromSeconds(int seconds) {
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
