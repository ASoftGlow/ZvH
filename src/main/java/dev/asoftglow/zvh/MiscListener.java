package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.util.Util;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MiscListener implements Listener
{
  @EventHandler
  public void onRespawn(PlayerRespawnEvent e)
  {
    var player = e.getPlayer();
    if (Game.isPlaying(player))
    {
      if (ZvH.humansTeam.hasPlayer(player))
      {
        Game.leaveHumans(player);
      }
      if (Game.getState() == Game.State.PLAYING)
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> ClassSelectionMenu.showTo(player));
    }
  }

  @EventHandler
  public void onDeath(PlayerDeathEvent e)
  {
    var player = e.getPlayer();
    if (Game.isPlaying(player))
    {
      Game.handleDeath(e);
    } else
    {
      ZvH.waitersTeam.removePlayer(player);
    }
  }

  @EventHandler
  public void onArrowHit(ProjectileHitEvent e)
  {
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
    if (!(Game.isPlaying(shooter) && Game.isPlaying(victim)))
      return;

    if (!(ZvH.humansTeam.hasPlayer(shooter) && ThreadLocalRandom.current().nextInt(2) == 0))
      return;
    shooter.getInventory().addItem(new ItemStack(Material.ARROW));
  }

  @EventHandler
  public void onDamage(EntityDamageEvent e)
  {
    if (e.getEntityType() == EntityType.PLAYER)
    {
      var player = (Player) e.getEntity();
      if (!Game.isPlaying(player))
        return;
      var damager = e.getDamageSource().getCausingEntity();
      if (damager == null || !(damager instanceof Player))
        return;
      Combat.handleDamage(player, (Player) damager);
    }
  }

  @EventHandler
  public void onAttackNpc(PrePlayerAttackEntityEvent e)
  {
    if (e.getAttacked().getType() == EntityType.SLIME)
      e.setCancelled(true);
    else
    {
      onClickNpc(e, e.getAttacked());
    }
  }

  @EventHandler
  public void onInteractNpc(PlayerInteractAtEntityEvent e)
  {
    if (e.getHand().equals(EquipmentSlot.HAND) && e.getPlayer().getGameMode() != GameMode.SPECTATOR)
    {
      if (e.getRightClicked().getScoreboardTags().contains("npc"))
        e.getPlayer().swingMainHand();
      if (e.getRightClicked() instanceof Player)
      {
        e.getPlayer().swingMainHand();
        ((Player) e.getRightClicked()).playSound(e.getPlayer().getLocation(), Sound.ENTITY_CAT_PURREOW, 0.5f, 1.5f);
        e.getPlayer().playSound(((Player) e.getRightClicked()).getLocation(), Sound.ENTITY_CAT_PURREOW, 0.5f, 1.5f);
      }
      onClickNpc(e, e.getRightClicked());
    }
  }

  public <E extends PlayerEvent & Cancellable> void onClickNpc(E e, Entity target)
  {
    if (!Game.isPlaying(e.getPlayer()))
    {
      if (target.getScoreboardTags().contains("npc"))
      {
        e.setCancelled(true);
        switch (ZvH.plainSerializer.serialize(target.customName()))
        {
        case "Play":
          Game.play(e.getPlayer());
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
  public void onItemInteract(PlayerInteractEvent e)
  {
    var player = e.getPlayer();
    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
    {
      if (e.getItem() == null)
        return;
      if (e.getItem().equals(Game.spec_leave_item))
      {
        Game.leaveSpectators(player);
      } else if (e.getItem().equals(ShopMenu.tracker))
      {
        var nearest = Util.getClosestTeamMember(player, ZvH.humansTeam);
        if (nearest == null)
        {
          Util.playSound(player, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8f, 1f);
          player.sendActionBar(Component.text("Tracker calibration error", NamedTextColor.RED));
          return;
        }
        player.setCompassTarget(nearest.getLocation());
        Util.playSound(player, Sound.BLOCK_PISTON_CONTRACT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("Calibrated tracker", NamedTextColor.GRAY));
      } else if (e.getItem().getType() == Material.GUNPOWDER)
      {
        if (e.getClickedBlock() != null)
        {
          if (e.getClickedBlock().getType() == Material.BARRIER
              && e.getClickedBlock().getLocation().distanceSquared(e.getPlayer().getLocation()) < 5)
          {
            e.getPlayer().sendMessage(Component.text("You are too close to the barrier!").color(NamedTextColor.RED));
            return;
          }
        }
        if (player.getGameMode() != GameMode.CREATIVE)
          e.getItem().setAmount(e.getItem().getAmount() - 1);
        var spawnPos = player.getLocation();
        spawnPos.setY(spawnPos.getY() + 1d);
        var tnt = (TNTPrimed) ZvH.world.spawnEntity(spawnPos, EntityType.PRIMED_TNT, false);
        tnt.setFuseTicks(60);
        tnt.setVelocity(spawnPos.getDirection().multiply(2.2f));
        tnt.setSource(player);
        for (var p : Bukkit.getOnlinePlayers())
          p.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
      }
    }
  }

  @EventHandler
  public void onConsume(PlayerItemConsumeEvent e)
  {
    var item = e.getItem();
    if (item.getType() == Material.POTION && e.getPlayer().getGameMode() != GameMode.CREATIVE)
    {
      Bukkit.getScheduler().runTask(ZvH.singleton, () -> e.getPlayer().getInventory().remove(Material.GLASS_BOTTLE));
    }
  }

  @EventHandler @SuppressWarnings("deprecation")
  public void onArrowPickUp(PlayerPickupArrowEvent e)
  {
    if (ZvH.humansTeam.hasPlayer(e.getPlayer())
        && ZvH.zombiesTeam.hasPlayer(Bukkit.getPlayer(e.getArrow().getOwnerUniqueId())))
    {
      if (e.getArrow().getUniqueId().toString().charAt(0) % 2 != 0)
      {
        e.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onFish(PlayerFishEvent e)
  {
    switch (e.getState())
    {
    case CAUGHT_FISH:
      ((Item) e.getCaught()).setItemStack(null);
      e.setExpToDrop(0);

      final int floor = 3;
      final int range = 3;
      var v = ThreadLocalRandom.current().nextInt(range + floor + 1) - floor;
      if (v > 0)
      {
        Util.playSoundAllAt(e.getPlayer(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f,
            0.5f + (float) v / (float) range);
        ZvH.changeCoins(e.getPlayer(), v * v, "fishing");
      } else
      {
        e.getPlayer().sendActionBar(Component.text("Nothing..."));
      }
      break;
    default:
      break;
    }
  }
}
