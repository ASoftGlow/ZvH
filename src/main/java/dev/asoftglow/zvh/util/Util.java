package dev.asoftglow.zvh.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.Team;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public abstract class Util
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
      String ename = plainSerializer.serialize(entity.customName());
      if (ename.equals(name))
        return entity;
    }
    return null;
  }

  public static List<Player> getPlayersWithTag(String tag)
  {
    List<Player> playersWithTag = new ArrayList<>();

    for (var player : Bukkit.getOnlinePlayers())
    {
      if (player.getScoreboardTags().contains(tag))
      {
        playersWithTag.add(player);
      }
    }

    return playersWithTag;
  }

  public static void playSound(Player player, Sound sound, float volume, float pitch)
  {
    player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
  }

  public static void playSoundAll(Sound sound, float volume, float pitch)
  {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> {
      playSound(p, sound, volume, pitch);
    });
  }

  public static <T> T popRandom(Set<T> set)
  {
    var choice = set.stream().skip(ThreadLocalRandom.current().nextInt(set.size())).findAny().get();
    set.remove(choice);
    return choice;
  }

  public static void givePotionEffect(Player player, PotionEffect e)
  {
    player.getServer().dispatchCommand(player.getServer().getConsoleSender(),
        // Can't disable potion particles without manual packet modification
        "effect give %s %s infinite %d true".formatted(player.getName(), e.getType().key().asString(),
            e.getAmplifier()));
  }

  public static void sendServerMsg(String content)
  {
    var c = Component.text("> ").append(Component.text(content, msg_style));
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

  public static Player getClosestTeamMember(Player player, Team team)
  {
    Player result = null;
    double lastDistance = Double.MAX_VALUE;
    for (var e : team.getEntries())
    {
      var p = Bukkit.getPlayerExact(e);
      if (p == null || p == player)
        continue;

      double distance = player.getLocation().distanceSquared(p.getLocation());
      if (distance < lastDistance)
      {
        lastDistance = distance;
        result = p;
      }
    }
    return result;
  }
}
