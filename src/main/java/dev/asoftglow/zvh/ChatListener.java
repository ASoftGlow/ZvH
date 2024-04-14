package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatListener implements Listener {
  @EventHandler
  public void onChatMsg(AsyncChatEvent e) {
    // e.setCancelled(true);
  }
}
