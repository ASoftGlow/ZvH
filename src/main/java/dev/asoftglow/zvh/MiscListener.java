package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
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
    if (Game.playerIsPlaying(player)) {
      if (ZvH.humansTeam.hasPlayer(player)) {
        Game.leaveHumans(player);
      }
      if (Game.isActive())
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> ClassSelectionMenu.showTo(player));
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e) {
    var player = e.getPlayer();
    if (Game.playerIsPlaying(player)) {
      if (ZvH.humansTeam.hasPlayer(player) && Game.getHumansCount() == 1) {
        e.setCancelled(true);
        for (var p : Game.playing) {
          p.setGameMode(GameMode.ADVENTURE);

          var ploc = p.getLocation();
          ploc.setY(ploc.getY() + 0.6d);
          var loc = new Location(p.getWorld(),
              ploc.getX() - 0.5d,
              ploc.getY() - 0.1d,
              ploc.getZ() - 0.5d);

          var marker = p.getWorld().createEntity(ploc, ArmorStand.class);
          marker.getScoreboardTags().add("temp");
          marker.setMarker(true);
          marker.setInvisible(true);
          marker.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(0.1f);
          marker.addPassenger(p);

          var carpet = p.getWorld().createEntity(loc, BlockDisplay.class);
          carpet.setBlock(Material.PURPLE_CARPET.createBlockData());
          carpet.getScoreboardTags().add("temp");

          marker.spawnAt(marker.getLocation());
          carpet.spawnAt(carpet.getLocation());
        }
        player.getServer().getServerTickManager().setFrozen(true);
        Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
          for (var e2 : ZvH.world.getEntities()) {
            if (e2.getScoreboardTags().contains("temp"))
              e2.remove();
          }
          e.callEvent();
          Game.stop();
          player.getServer().getServerTickManager().setFrozen(false);
        }, 20 * 5);
      }
      if (player.getKiller() != null && Game.playerIsPlaying(player.getKiller())) {
        var s = ZvH.xp.getScore(player.getKiller());
        var ns = s.getScore() + 10;
        s.setScore(ns);
        var lvl = ZvH.lvl.getScore(player.getKiller());

        ZvH.changeCoins(player.getKiller(), ZvH.humansTeam.hasPlayer(player.getKiller()) ? 2 : 5);

        player.getKiller().setExp(0);
        player.getKiller().setLevel(0);
        player.getKiller().giveExp(ns, false);
        lvl.setScore(player.getKiller().getLevel());
        var wasArrow = player.getLastDamageCause().getCause().equals(DamageCause.PROJECTILE);

        if (ZvH.zombiesTeam.hasPlayer(player)) {
          player.getKiller().setHealth(Math.min(player.getKiller().getHealth() + (wasArrow ? 1d : 2d), 20d));
          player.getKiller().getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_brains")))
              .awardCriteria("killh");
        } else if (ZvH.humansTeam.hasPlayer(player)) {
          player.getKiller().getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_blood")))
              .awardCriteria("killz");
        }
      }
      player.getInventory().clear();
      player.setItemOnCursor(null);
      e.setDroppedExp(0);
      e.setNewTotalExp(0);
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

    if (!(ZvH.humansTeam.hasPlayer(shooter) && ThreadLocalRandom.current().nextInt(2) == 0))
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
      if (e.getRightClicked().getScoreboardTags().contains("npc"))
        e.getPlayer().swingMainHand();
      if (e.getRightClicked() instanceof Player) {
        e.getPlayer().swingMainHand();
        ((Player) e.getRightClicked()).playSound(e.getPlayer().getLocation(), Sound.ENTITY_CAT_PURREOW, 0.5f, 1.5f);
        e.getPlayer().playSound(((Player) e.getRightClicked()).getLocation(), Sound.ENTITY_CAT_PURREOW, 0.5f, 1.5f);
      }
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
    if (Game.isActive() && e.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
      if (e.getBlock().getLocation().getBlockY() == MapControl.current_size.bounds().y) {
        e.setCancelled(true);
      }
  }

  @EventHandler
  public void onItemInteract(PlayerInteractEvent e) {
    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
      if (e.getItem() != null) {
        if (e.getItem().equals(Game.spec_leave_item)) {
          Game.leaveSpectators(e.getPlayer());
        } else if (e.getItem().getType() == Material.GUNPOWDER) {
          if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
            e.getItem().setAmount(e.getItem().getAmount() - 1);
          var spawnPos = e.getPlayer().getLocation();
          spawnPos.setY(spawnPos.getY() + 1d);
          var tnt = (TNTPrimed) ZvH.world.spawnEntity(spawnPos, EntityType.PRIMED_TNT, false);
          tnt.setFuseTicks(60);
          tnt.setVelocity(spawnPos.getDirection().multiply(2.5f));
          for (var p : Bukkit.getOnlinePlayers())
            p.playSound(e.getPlayer().getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
        }
      }
    }
  }

  @EventHandler
  public void onSlimeSplit(SlimeSplitEvent e) {
    e.setCancelled(true);
  }

  private static Set<Material> explodeWhitelist = Set.of(Material.GRAVEL, Material.LIGHT_GRAY_WOOL, Material.LADDER,
      Material.COBWEB);

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent e) {
    if (e.getEntity().getType() == EntityType.PRIMED_TNT) {
      var destroyed = e.blockList();
      var it = destroyed.iterator();
      while (it.hasNext()) {
        var block = it.next();
        if (!explodeWhitelist.contains(block.getType()))
          it.remove();
      }
    }
  }

  @EventHandler
  public void onConsume(PlayerItemConsumeEvent e) {
    var item = e.getItem();
    if (item.getType() == Material.POTION && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
      Bukkit.getScheduler().runTask(ZvH.singleton, () -> e.getPlayer().getInventory().remove(Material.GLASS_BOTTLE));
    }
  }

  @EventHandler
  public void onDismount(EntityDismountEvent e) {
    if (e.getEntity() instanceof Player && e.getDismounted().getScoreboardTags().contains("m")) {
      e.setCancelled(true);
    }
  }
}
