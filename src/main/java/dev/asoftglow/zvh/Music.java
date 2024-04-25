package dev.asoftglow.zvh;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Music {
  // Make a map for player toggles
  static Map<Player, String> musics = new HashMap<>();

  public static void play(Player player, String song) {
    if (musics.get(player) != null) {
      stop(player);
    }
    musics.put(player, song);
    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
        "execute as " + player.getName() + " run function " + song + ":play");
  }

  public static void stop(Player player) {
    if (musics.get(player) == null)
      return;
    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
        "execute as " + player.getName() + " run function " + musics.get(player) + ":stop");
  }

  public static void playLobby(Player player) {
    play(player, "a_decaying_city");
  }
}
