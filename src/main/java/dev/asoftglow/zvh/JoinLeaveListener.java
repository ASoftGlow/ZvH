package dev.asoftglow.zvh;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;

public class JoinLeaveListener implements Listener {

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    var player = e.getPlayer();
    e.joinMessage(Component.text("Welcome, %s!".formatted(player.getName())));
  }

  @EventHandler
  public void onLeave(PlayerQuitEvent e) {
    var player = e.getPlayer();
    e.quitMessage(Component.text("Goodbye, %s...".formatted(player.getName())));
  }
}
