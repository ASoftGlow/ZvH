package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;

import java.util.function.Consumer;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MiscListener implements Listener {
  private PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

  @EventHandler
  public void onChatMsg(AsyncChatEvent e) {
    e.message(e.message().color(NamedTextColor.GOLD));
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    var player = e.getPlayer();
    if (Game.playerIsPlaying(player)) {
      if (ZvH.humansTeam.hasPlayer(player)) {
        Game.leaveHumans(player);
      } else {
        player.teleport(MapControl.getLocation(player, MapControl.mapSizes[0].zombieSpawn()));
        ClassSelectionMenu.showTo(player);
      }

      player.getInventory().clear();
      e.setDroppedExp(0);
      e.setNewTotalExp(0);
    } else {
      ZvH.waitersTeam.removePlayer(player);
    }
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
      e.setCancelled(true);
  }

  @EventHandler
  public void onClickNpc(PrePlayerAttackEntityEvent e) {
    onClickNpc(e, e.getAttacked());
  }

  @EventHandler
  public void onInteractNpc(PlayerInteractAtEntityEvent e) {
    if (e.getHand().equals(EquipmentSlot.HAND)) {
      e.getPlayer().swingMainHand();
      onClickNpc(e, e.getRightClicked());
    }
  }

  @EventHandler
  public void onArmor(PlayerArmorChangeEvent e) {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE && e.getNewItem().getType() == Material.AIR) {
      var inv = e.getPlayer().getInventory();
      Consumer<ItemStack> m = switch (e.getSlotType()) {
        case CHEST -> inv::setChestplate;
        case FEET -> inv::setBoots;
        case HEAD -> inv::setHelmet;
        case LEGS -> inv::setLeggings;
      };
      m.accept(e.getOldItem());
      inv.remove(e.getOldItem());
      e.getPlayer().setItemOnCursor(null);
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
}
