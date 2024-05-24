package dev.asoftglow.zvh;

import net.dv8tion.jda.api.EmbedBuilder;

/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.kyori.adventure.text.Component;

import javax.annotation.Nonnull;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;

public class DiscordBot extends ListenerAdapter
{
  private static final long guildId = 1241443425426083910L;
  private static boolean isReady = false;
  private static JDA jda;
  private static TextChannel chatChannel;
  private static TextChannel statusChannel;
  private static BiConsumer<String, String> messageHandler;

  private DiscordBot()
  {
    super();
  }

  public static void setMessageHandler(BiConsumer<String, String> handler)
  {
    messageHandler = handler;
  }

  public static void sendMessage(Component msg)
  {
    sendMessage(ZvH.plainSerializer.serialize(msg));
  }

  public static void sendMessage(String msg)
  {
    if (!isReady)
      return;

    var b = new MessageCreateBuilder().setContent(msg).setSuppressedNotifications(true).setAllowedMentions(Set.of());
    chatChannel.sendMessage(b.build()).queue();
  }

  public static void sendMessageAndWait(String msg)
  {
    if (!isReady)
      return;

    var b = new MessageCreateBuilder().setContent(msg).setSuppressedNotifications(true).setAllowedMentions(Set.of());
    chatChannel.sendMessage(b.build()).complete();
  }

  public static void setStatus(String status, int players)
  {
    if (!isReady)
      return;

    var embed = new EmbedBuilder().setTitle(status);
    embed.addField("Players", Integer.toString(players), false);

    statusChannel.retrieveMessageById(statusChannel.getLatestMessageIdLong()).queue((msg) -> {
      msg.editMessageEmbeds(embed.build()).queue();
    }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (e) -> {
      statusChannel.sendMessageEmbeds(embed.build()).queue();
    }));

    jda.getPresence().setActivity(
        Activity.customStatus(players == 1 ? "1 player online" : Integer.toString(players) + " players online"));
  }

  public static void stop()
  {
    if (!isReady)
      return;
    isReady = false;
    setStatus("Offline", 0);
    sendMessageAndWait("```swift\nShutting down...```");
    jda.shutdown();
  }

  public static void login(String token)
  {
    // Pick which intents we need to use in our code.
    // To get the best performance, you want to make the most minimalistic list of
    // intents, and have all others disabled.
    // When an intent is enabled, you will receive events and cache updates related
    // to that intent.
    // For more information:
    //
    // - The documentation for GatewayIntent:
    // https://docs.jda.wiki/net/dv8tion/jda/api/requests/GatewayIntent.html
    // - The wiki page for intents and caching:
    // https://jda.wiki/using-jda/gateway-intents-and-member-cache-policy/

    EnumSet<GatewayIntent> intents = EnumSet.of(
        // Enables MessageReceivedEvent for guild (also known as servers)
        GatewayIntent.GUILD_MESSAGES,
        // Enables the event for private channels (also known as direct messages)
        // GatewayIntent.DIRECT_MESSAGES,
        // Enables access to message.getContentRaw()
        GatewayIntent.MESSAGE_CONTENT
    // Enables MessageReactionAddEvent for guild
    // GatewayIntent.GUILD_MESSAGE_REACTIONS,
    // Enables MessageReactionAddEvent for private channels
    // GatewayIntent.DIRECT_MESSAGE_REACTIONS
    );

    try
    {
      // By using createLight(token, intents), we use a minimalistic cache profile
      // (lower ram usage)
      // and only enable the provided set of intents. All other intents are disabled,
      // so you won't receive events for those.
      jda = JDABuilder.createLight(token, intents)
          // On this builder, you are adding all your event listeners and session
          // configuration
          .addEventListeners(new DiscordBot())
          // Once you're done configuring your jda instance, call build to start and login
          // the bot.
          .build();

      // Here you can now start using the jda instance before its fully loaded,
      // this can be useful for stuff like creating background services or similar.

      // The queue(...) means that we are making a REST request to the discord API
      // server!
      // Usually, this is done asynchronously on another thread which handles
      // scheduling and rate-limits.
      jda.getRestPing().queue(ping ->
      // shows ping in milliseconds
      System.out.println("Logged in with ping: " + ping));

      // If you want to access the cache, you can use awaitReady() to block the main
      // thread until the jda instance is fully loaded
      jda.awaitReady();
      isReady = true;
      final var guild = jda.getGuildById(guildId);
      if (guild == null)
      {
        System.err.println("Guild not found");
        stop();
        return;
      }
      chatChannel = guild.getTextChannelById(1241443831795417251L);
      statusChannel = guild.getTextChannelById(1241474029483720867L);
      setStatus("Online", ZvH.getPlayerCount());
      sendMessage("```md\n> Online```");

    } catch (InterruptedException e)
    {
      // Thrown if the awaitReady() call is interrupted
      e.printStackTrace();
    }
  }

  @Override
  public void onMessageReceived(@Nonnull MessageReceivedEvent event)
  {
    if (!event.isFromGuild() || event.getGuild().getIdLong() != guildId)
      return;
    if (event.getAuthor().getIdLong() == jda.getSelfUser().getIdLong())
      return;

    if (event.getChannel().getIdLong() == chatChannel.getIdLong())
    {
      messageHandler.accept(event.getAuthor().getEffectiveName(), event.getMessage().getContentDisplay());
    }
    // staff channel
    else if (event.getChannel().getIdLong() == 1241480641598717972L)
    {
      if (event.getMessage().getContentRaw().startsWith("!cmd "))
      {
        ZvH.CMD = event.getMessage().getContentRaw().substring(5);
      }
    }
  }
}