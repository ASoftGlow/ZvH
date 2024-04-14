package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class Util {
  public static Entity findEntity(String name, World world) {
    PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
    for (var entity : world.getEntities()) {
      String ename = plainSerializer.serialize(entity.customName());
      if (ename.equals(name))
        return entity;
    }
    return null;
  }

  public static List<Player> getPlayersWithTag(String tag) {
    List<Player> playersWithTag = new ArrayList<>();

    for (var player : Bukkit.getOnlinePlayers()) {
      if (player.getScoreboardTags().contains(tag)) {
        playersWithTag.add(player);
      }
    }

    return playersWithTag;
  }

  public static void playSound(Player player, Sound sound, float volume, float pitch) {
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER, 0.8f, 2f);
  }

  public static void playSoundAll(Sound sound, float volume, float pitch) {
    Bukkit.getServer().getOnlinePlayers().forEach(p -> {
      playSound(p, sound, volume, pitch);
    });
  }

  public static <T> T popRandom(Set<T> set) {
    var choice = set.stream()
        .skip(ThreadLocalRandom.current()
            .nextInt(set.size()))
        .findAny().get();
    set.remove(choice);
    return choice;
  }
}
