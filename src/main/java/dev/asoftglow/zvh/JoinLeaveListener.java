package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class JoinLeaveListener implements Listener
{
  @EventHandler
  public void onJoin(PlayerJoinEvent e)
  {
    var player = e.getPlayer();
    if (player.getName().equals("Bluentage"))
    {
      player.displayName(player.displayName().color(NamedTextColor.RED));
    } else if (player.isOp())
    {
      Moderation.vanishTo(player);
      player.displayName(player.displayName().color(NamedTextColor.GOLD));
    }

    e.joinMessage(Component.text("Welcome, ").append(player.displayName()).append(Component.text("!")));
    if (!player.hasPlayedBefore())
    {
      player.getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:root"))).awardCriteria("join");
      GuideBook.showTo(player);
    } else
    {
      player.sendMessage(ZvH.reminderMsg);
    }
    Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
      player.sendMessage(
          Component.text("\nNOTE: This is a new update in testing. It is expected that coins and levels are reset.\n",
              NamedTextColor.RED));
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1, 1.5f);
    }, 20 * 3);

    Database.fetchPlayer(player);
    SideBoard.addBoard(player);

    Util.playSoundAll(Sound.UI_TOAST_IN, 1f, 1.5f);
    Music.stop(player);
    Music.playLobby(player);
    DiscordBot.sendMessage("Welcome, %s!".formatted(player.getName()));
    DiscordBot.setStatus("Online", ZvH.getPlayerCount());
  }

  @EventHandler
  public void onLeave(PlayerQuitEvent e)
  {
    var player = e.getPlayer();
    SideBoard.removeBoard(player);
    Database.cleanCacheFor(player);
    Game.leave(player, true);
    Music.stop(player);

    e.quitMessage(Component.text("Goodbye, %s...".formatted(player.getName())));
    Util.playSoundAll(Sound.UI_TOAST_OUT, 1f, 1.5f);
    DiscordBot.sendMessage("Goodbye, %s...".formatted(player.getName()));
    DiscordBot.setStatus("Online", ZvH.getPlayerCount() - 1);
  }
}
