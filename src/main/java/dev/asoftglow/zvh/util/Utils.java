package dev.asoftglow.zvh.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;

import net.kyori.adventure.sound.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;

import dev.asoftglow.zvh.DiscordBot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public abstract class Utils
{
  private static final Style msg_style = Style.style(NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);
  private static final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

  public static Entity findEntity(String name, World world)
  {
    for (var entity : world.getEntities())
    {
      String ename = plainSerializer.serialize(entity.customName());
      if (ename.equals(name))
        return entity;
    }
    return null;
  }

  public static <T extends Entity> T findEntity(String name, Class<T> type, World world)
  {
    for (var entity : world.getEntitiesByClass(type))
    {
      if (entity.customName() != null)
        if (plainSerializer.serialize(entity.customName()).equals(name))
          return entity;
    }
    return null;
  }

  public static void playSound(HumanEntity player, org.bukkit.Sound sound, float volume, float pitch)
  {
    player.playSound(Sound.sound(sound, Sound.Source.MASTER, volume, pitch), Sound.Emitter.self());
  }

  public static void playSound(HumanEntity player, org.bukkit.Sound sound)
  {
    playSound(player, sound, 1, 1);
  }

  public static void playSoundAll(org.bukkit.Sound sound, float volume, float pitch)
  {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> {
      playSound(p, sound, volume, pitch);
    });
  }

  public static void playSoundAllAt(Entity entity, org.bukkit.Sound sound, float volume, float pitch)
  {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> {
      p.playSound(entity, sound, volume, pitch);
    });
  }

  public static <T> T popRandom(Set<T> set)
  {
    var choice = set.stream().skip(ThreadLocalRandom.current().nextInt(set.size())).findAny().get();
    set.remove(choice);
    return choice;
  }

  public static void sendServerMsg(String content)
  {
    var c = Component.text("> ").append(Component.text(content, msg_style));
    for (var p : Bukkit.getOnlinePlayers())
      p.sendMessage(c);
    DiscordBot.sendMessage("```" + content + "```");
  }

  public static void sendServerMsg(Component content)
  {
    var c = Component.text("> ").append(Component.empty().style(msg_style).append(content));
    for (var p : Bukkit.getOnlinePlayers())
      p.sendMessage(c);
  }

  public static Set<Player> getTeamPlayers(Team team)
  {
    var players = new HashSet<Player>();
    for (var e : team.getEntries())
    {
      var player = Bukkit.getPlayerExact(e);
      if (player != null)
        players.add(player);
    }
    return players;
  }

  public static int getTeamPlayersCount(Team team)
  {
    int players = 0;
    for (var e : team.getEntries())
    {
      var player = Bukkit.getPlayerExact(e);
      if (player != null)
        players++;
    }
    return players;
  }

  public static <T> T pickRandom(List<T> list)
  {
    return list.get(ThreadLocalRandom.current().nextInt(list.size()));
  }

  public static Player getClosestTeamMember(Player player, Set<Player> team)
  {
    Player result = null;
    double lastDistance = Double.MAX_VALUE;
    for (var p : team)
    {
      double distance = player.getLocation().distanceSquared(p.getLocation());
      if (distance < lastDistance)
      {
        lastDistance = distance;
        result = p;
      }
    }
    return result;
  }

  public static void addLore(ItemMeta itemMeta, Component lore)
  {
    if (itemMeta.hasLore())
    {
      var old_lore = itemMeta.lore();
      old_lore.add(lore);
      itemMeta.lore(old_lore);

    } else
    {
      itemMeta.lore(List.of(lore));
    }
  }

  public static Collection<Player> getPlayers(Collection<UUID> uuids)
  {
    var players = new ArrayList<Player>();
    for (var u : uuids)
    {
      Player p = Bukkit.getPlayer(u);
      if (p != null && p.isConnected())
      {
        players.add(p);
      }
    }
    return players;
  }

  public static int getMaxI(int[] array)
  {
    int i = 0;
    int max = array[i];

    for (int j = 0; j < array.length; j++)
    {
      if (array[j] > max)
      {
        i = j;
        max = array[j];
      }
    }
    return i;
  }
}
