package dev.asoftglow.zvh;

import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;

public class JoinLeaveListener implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    var player = e.getPlayer();
    e.joinMessage(Component.text("Welcome, %s!".formatted(player.getName())));
    Util.playSoundAll(Sound.UI_TOAST_IN, 1f, 1.5f);
    Game.addBoard(player);
  }

  @EventHandler
  public void onLeave(PlayerQuitEvent e) {
    var player = e.getPlayer();
    Game.removeBoard(player);
    Game.leave(player);

    e.quitMessage(Component.text("Goodbye, %s...".formatted(player.getName())));
    Util.playSoundAll(Sound.UI_TOAST_OUT, 1f, 1.5f);
  }
}
