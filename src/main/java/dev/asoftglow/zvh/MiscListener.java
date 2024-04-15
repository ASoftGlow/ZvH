package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MiscListener implements Listener {
  private PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

  @EventHandler
  public void onChatMsg(AsyncChatEvent e) {
    // e.message(e.message().color(NamedTextColor.GOLD));
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e) {
    var player = e.getPlayer();
    if (player.getScoreboardTags().remove("needs-tp")) {
      player.teleport(ZvH.worldSpawnLocation);
    }
    if (Game.playerIsPlaying(player)) {
      if (ZvH.zombiesTeam.hasPlayer(player)) {
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> ClassSelectionMenu.showTo(player));
      }
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    var player = e.getPlayer();
    if (Game.playerIsPlaying(player)) {
      if (player.getKiller() != null && Game.playerIsPlaying(player.getKiller())) {
        var s = ZvH.xp.getScore(player.getKiller());
        var ns = s.getScore() + 10;
        s.setScore(ns);
        player.getKiller().setTotalExperience(ns);
      }
      player.getInventory().clear();
      player.setItemOnCursor(null);
      e.setDroppedExp(0);
      e.setNewTotalExp(0);
      if (ZvH.humansTeam.hasPlayer(player)) {
        Game.leaveHumans(player);
      }
    } else {
      ZvH.waitersTeam.removePlayer(player);
    }
  }

  @EventHandler
  public void onArrowHit(ProjectileHitEvent e) {
    if (e.getHitEntity() == null)
      return;
    if (!(e.getHitEntity() instanceof Player))
      return;
    var victim = (Player) e.getHitEntity();
    var uuid = e.getEntity().getOwnerUniqueId();
    if (uuid == null)
      return;
    var shooter = Bukkit.getPlayer(uuid);
    if (shooter == null)
      return;
    if (!(Game.playerIsPlaying(shooter) && Game.playerIsPlaying(victim)))
      return;

    if (!(ZvH.humansTeam.hasPlayer(shooter) && ThreadLocalRandom.current().nextInt(11) == 0))
      return;
    shooter.getInventory().addItem(new ItemStack(Material.ARROW));
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
      e.setCancelled(true);
  }

  @EventHandler
  public void onAttackNpc(PrePlayerAttackEntityEvent e) {
    if (e.getAttacked().getType() == EntityType.SLIME)
      e.setCancelled(true);
    else
      onClickNpc(e, e.getAttacked());
  }

  @EventHandler
  public void onInteractNpc(PlayerInteractAtEntityEvent e) {
    if (e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
      if (e.getRightClicked() instanceof Player)
        e.getPlayer().swingMainHand();
      else
        onClickNpc(e, e.getRightClicked());
    }
  }

  public <E extends PlayerEvent & Cancellable> void onClickNpc(E e, Entity target) {
    if (!Game.playerIsPlaying(e.getPlayer())) {
      if (target.getScoreboardTags().contains("npc")) {
        e.setCancelled(true);
        switch (plainSerializer.serialize(target.customName())) {
          case "Play":
            if (!Game.isActive()) {
              if (ZvH.waitersTeam.hasPlayer(e.getPlayer())) {
                Game.leaveWaiters(e.getPlayer());
                e.getPlayer().sendMessage("Left queue");
              } else {
                Game.joinWaiters(e.getPlayer());
                e.getPlayer().sendMessage("Joined queue");
              }
            } else {
              Game.joinZombies(e.getPlayer());
            }
            break;

          case "Spec":
            Game.joinSpectators(e.getPlayer());
            break;

          default:
            break;
        }
      }
    }
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e) {
    if (e.getBlock().getLocation().getBlockY() == 1) {
      if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
        e.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onItemInteract(PlayerInteractEvent e) {
    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
      if (e.getItem() != null && e.getItem().equals(Game.spec_leave_item)) {
        Game.leaveSpectators(e.getPlayer());
      }
    }
  }

  @EventHandler
  public void onSlimeSplit(SlimeSplitEvent e) {
    e.setCancelled(true);
  }
}
