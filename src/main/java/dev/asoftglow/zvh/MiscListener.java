package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.util.Util;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class MiscListener implements Listener
{
  private PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();

  @EventHandler
  public void onChatMsg(AsyncChatEvent e)
  {
    // e.message(e.message().color(NamedTextColor.GOLD));
  }

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
      if (Game.isActive())
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> ClassSelectionMenu.showTo(player));
    }
  }

  private boolean doingDeathAnimation = false;

  @EventHandler
  public void onDeath(PlayerDeathEvent e)
  {
    var player = e.getPlayer();
    if (Game.isPlaying(player))
    {
      // kill rewards
      Game.handleKillRewards(player);

      // last human
      if (ZvH.humansTeam.hasPlayer(player) && Game.getHumansCount() == 1)
      {
        if (doingDeathAnimation)
          return;
        doingDeathAnimation = true;
        final var tm = player.getServer().getServerTickManager();
        // keep player alive
        // TODO: increment stat
        e.setCancelled(true);
        tm.setFrozen(true);

        for (var p : Game.playing)
        {
          p.setGameMode(GameMode.ADVENTURE);

          var ploc = p.getLocation();
          ploc.setY(ploc.getY() + 0.6d);
          var loc = new Location(p.getWorld(), ploc.getX() - 0.5d, ploc.getY() - 0.1d, ploc.getZ() - 0.5d);

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
        // allow carpets to appear
        tm.stepGameIfFrozen(1);

        Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
          for (var e2 : ZvH.world.getEntities())
          {
            if (e2.getScoreboardTags().contains("temp"))
              e2.remove();
          }
          Game.zombiesWin();
          tm.setFrozen(false);
          doingDeathAnimation = false;
        }, 20 * 5);
      }
      player.getInventory().clear();
      player.setItemOnCursor(null);
      e.setDroppedExp(0);
      e.setNewTotalExp(0);
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
  public void onDrop(PlayerDropItemEvent e)
  {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
      e.setCancelled(true);
  }

  @EventHandler
  public void onAttackNpc(PrePlayerAttackEntityEvent e)
  {
    if (e.getAttacked().getType() == EntityType.SLIME)
      e.setCancelled(true);
    else
    {
      onClickNpc(e, e.getAttacked());
      if (e.getAttacked() instanceof Player && Game.isPlaying(e.getPlayer()) && doingDeathAnimation)
      {
        e.setCancelled(true);
      }
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
        switch (plainSerializer.serialize(target.customName()))
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
  public void onBlockBreak(BlockBreakEvent e)
  {
    if (Game.isActive() && e.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
      if (e.getBlock().getLocation().getBlockY() == MapControl.current_size.bounds().y)
      {
        e.setCancelled(true);
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
          player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8f, 1f);
          player.sendActionBar(Component.text("Tracker calibration error", NamedTextColor.RED));
          return;
        }
        player.setCompassTarget(nearest.getLocation());
        player.playSound(player.getLocation(), Sound.BLOCK_PISTON_CONTRACT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("Calibrated tracker", NamedTextColor.GRAY));
      } else if (e.getItem().getType() == Material.GUNPOWDER)
      {
        if (player.getGameMode() != GameMode.CREATIVE)
          e.getItem().setAmount(e.getItem().getAmount() - 1);
        var spawnPos = player.getLocation();
        spawnPos.setY(spawnPos.getY() + 1d);
        var tnt = (TNTPrimed) ZvH.world.spawnEntity(spawnPos, EntityType.PRIMED_TNT, false);
        tnt.setFuseTicks(60);
        tnt.setVelocity(spawnPos.getDirection().multiply(2.5f));
        for (var p : Bukkit.getOnlinePlayers())
          p.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
      }
    }
  }

  @EventHandler
  public void onSlimeSplit(SlimeSplitEvent e)
  {
    e.setCancelled(true);
  }

  private static Set<Material> explodeWhitelist = Set.of(Material.GRAVEL, Material.LIGHT_GRAY_WOOL, Material.LADDER,
      Material.COBWEB);

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent e)
  {
    if (e.getEntity().getType() == EntityType.PRIMED_TNT)
    {
      var destroyed = e.blockList();
      var it = destroyed.iterator();
      while (it.hasNext())
      {
        var block = it.next();
        if (!explodeWhitelist.contains(block.getType()))
          it.remove();
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

  @EventHandler
  public void onDismount(EntityDismountEvent e)
  {
    if (e.getEntity() instanceof Player && e.getDismounted().getScoreboardTags().contains("temp"))
    {
      e.setCancelled(true);
    }
  }
}
