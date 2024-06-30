package dev.asoftglow.zvh;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import dev.asoftglow.zvh.util.Logger;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.chat.ChatType;
import net.kyori.adventure.text.Component;

public class Moderation implements Listener
{
  private static JSONArray config;
  private static File configFile;
  private static final JSONParser parser = new JSONParser();

  public static void setConfigFile(File config)
  {
    if (config == null || !config.canRead() || !config.canWrite())
      throw new IllegalArgumentException();
    configFile = config;
    loadConfig();
  }

  public static void loadConfig()
  {
    try
    {
      config = (JSONArray) parser.parse(new FileReader(configFile));
    } catch (IOException | ParseException e)
    {
      Logger.Get().log(Level.SEVERE, "Failed to read moderation config file!");
      e.printStackTrace();
    }
  }

  public static void saveConfig()
  {
    try (var writer = new FileWriter(configFile))
    {
      writer.write(config.toJSONString());
    } catch (IOException e)
    {
      Logger.Get().log(Level.SEVERE, "Failed to write moderation config file!");
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public static void mute(OfflinePlayer player)
  {
    config.add(player.getUniqueId().toString());
    saveConfig();
  }

  public static void unmute(OfflinePlayer player)
  {
    for (var o : config)
    {
      if (player.getUniqueId().toString().equals(o))
      {
        config.remove(o);
        saveConfig();
        return;
      }
    }
  }

  public static OfflinePlayer[] getMuted()
  {
    var players = new OfflinePlayer[config.size()];
    for (int i = 0; i < players.length; i++)
    {
      players[i] = Bukkit.getOfflinePlayer(UUID.fromString((String) config.get(i)));
    }
    return players;
  }

  public static boolean isMuted(Player player)
  {
    for (var o : config)
    {
      if (player.getUniqueId().toString().equals(o))
        return true;
    }
    return false;
  }

  @EventHandler
  public static void onChatMsg(AsyncChatEvent e)
  {
    if (Moderation.isMuted(e.getPlayer()))
    {
      e.setCancelled(true);
      e.getPlayer().sendMessage(e.signedMessage(), ChatType.CHAT.bind(e.getPlayer().name()));
      Logger.Get().info(e.getPlayer().getName() + " tried to speak: " + ZvH.plainSerializer.serialize(e.message()));
    } else
    {
      DiscordBot.sendMessage(Component.text("<" + e.getPlayer().getName() + "> ").append(e.message()));
    }
  }

  private static final Set<String> mutableCommands = Set.of("/msg", "/teammsg", "/w", "/tell");

  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent e)
  {
    final var cmd = e.getMessage().split(" ")[0];
    if (mutableCommands.contains(cmd) && isMuted(e.getPlayer()))
    {
      e.setCancelled(true);
    }
  }

  private static final Set<UUID> vanished_players = new HashSet<>();

  public static void vanish(Player player)
  {
    if (vanished_players.add(player.getUniqueId()))
    {
      for (var p : Bukkit.getOnlinePlayers())
      {
        if (!p.isOp())
        {
          p.hidePlayer(ZvH.singleton, player);
        }
      }
    }
  }

  public static void unvanish(Player player)
  {
    if (vanished_players.remove(player.getUniqueId()))
    {
      for (var p : Bukkit.getOnlinePlayers())
      {
        if (!p.isOp())
        {
          p.showPlayer(ZvH.singleton, player);
        }
      }
    }
  }

  public static void toggleVanish(Player player)
  {
    if (vanished_players.contains(player.getUniqueId()))
    {
      unvanish(player);
    } else
    {
      vanish(player);
    }
  }

  public static void vanishTo(Player player)
  {
    for (var u : vanished_players)
    {
      player.hidePlayer(ZvH.singleton, Bukkit.getPlayer(u));
    }
  }
}
