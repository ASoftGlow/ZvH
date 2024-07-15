package dev.asoftglow.zvh;

import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import dev.asoftglow.zvh.menus.ClassSelectionMenu;
import dev.asoftglow.zvh.menus.ShopMenu;
import dev.asoftglow.zvh.util.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MiscListener implements Listener
{
  private static final Map<Player, List<Integer>> life_tasks = new HashMap<>();

  public static void addPlayerLifeTask(Player player, BukkitTask task)
  {
    var l = life_tasks.get(player);
    if (l == null)
    {
      l = new ArrayList<>();
      life_tasks.put(player, l);

    }
    l.add(task.getTaskId());
  }

  public static void cancelPlayerLifeTasks(Player player)
  {
    var tasks = life_tasks.remove(player);
    if (tasks == null)
      return;
    for (var t : tasks)
    {
      Bukkit.getScheduler().cancelTask(t);
    }
  }

  public static void removeLifeTask(Player player, int task_id)
  {
    var l = life_tasks.get(player);
    if (l != null)
    {
      l.remove(Integer.valueOf(task_id));
    }
  }

  @EventHandler
  public void onRespawn(PlayerRespawnEvent e)
  {
    var player = e.getPlayer();
    if (Game.isPlaying(player))
    {
      if (Game.humans.contains(player))
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

    if (!(Game.humans.contains(shooter) && ThreadLocalRandom.current().nextInt(2) == 0))
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
      if (damager.getLocation().distanceSquared(player.getLocation()) < 16)
      {
        e.setDamage(e.getDamage() / 2);
      }
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
    final var player = e.getPlayer();
    if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
    {
      if (e.getItem() == null)
        return;

      if (e.getItem().equals(CustomItems.spec_leave))
      {
        Game.leaveSpectators(player);

      } else if (e.getItem().equals(CustomItems.tracker))
      {
        var nearest = Utils.getClosestTeamMember(player, Game.humans);
        if (nearest == null)
        {
          Utils.playSound(player, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8f, 1f);
          player.sendActionBar(Component.text("Tracker calibration error", NamedTextColor.RED));
          return;
        }
        player.setCompassTarget(nearest.getLocation());
        Utils.playSound(player, Sound.BLOCK_PISTON_CONTRACT, 0.8f, 1.5f);
        player.sendActionBar(Component.text("Calibrated tracker", NamedTextColor.GRAY));
        player.swingHand(e.getHand());

      } else if (e.getItem().equals(CustomItems.shop_open))
      {
        ShopMenu.handleCommand(player);
        player.swingHand(e.getHand());

      } else if (e.getItem().getType() == Material.GUNPOWDER)
      {
        if (e.getClickedBlock() != null)
        {
          if (e.getClickedBlock().getType() == Material.BARRIER
              && e.getClickedBlock().getLocation().distanceSquared(e.getPlayer().getLocation()) < 5)
          {
            player.sendMessage(Component.text("You are too close to the barrier!").color(NamedTextColor.RED));
            return;
          }
        }
        if (player.getGameMode() != GameMode.CREATIVE)
          e.getItem().setAmount(e.getItem().getAmount() - 1);
        var spawnPos = player.getLocation();
        spawnPos.setY(spawnPos.getY() + 1d);
        var tnt = ZvH.world.createEntity(spawnPos, TNTPrimed.class);
        tnt.setFuseTicks(60);
        tnt.setVelocity(spawnPos.getDirection().multiply(2.2f));
        tnt.setSource(player);
        tnt.spawnAt(spawnPos);

        for (var p : Bukkit.getOnlinePlayers())
          p.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1f, 1.5f);
        player.swingHand(e.getHand());

      } else if (e.getItem().equals(CustomItems.light_fuse))
      {
        player.swingHand(e.getHand());
        player.getInventory().remove(e.getItem());

        BukkitRunnable task = new BukkitRunnable()
        {
          int i = 4 + 1;

          public void run()
          {
            if (--i == 0)
            {
              player.sendActionBar(Component.text("BOOM!", NamedTextColor.RED));
              player.getWorld().createExplosion(player, 6f, false);
              player.setKiller(player);
              player.setHealth(0);
              cancel();
              removeLifeTask(player, getTaskId());
              return;
            }
            Utils.playSoundAllAt(player, Sound.ENTITY_CREEPER_PRIMED, 1f, (float) i / 2f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10, 0));
            player.sendActionBar(Component.text("Exploding in ", NamedTextColor.RED).append(Component.text(i)));
          }
        };

        addPlayerLifeTask(player, task.runTaskTimer(ZvH.singleton, 0, 20));
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
    if (Game.humans.contains(e.getPlayer()) && Game.zombies.contains(Bukkit.getPlayer(e.getArrow().getOwnerUniqueId())))
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
      // max(0, random(0 to 6) - 3)^2
      if (v > 0)
      {
        Utils.playSoundAllAt(e.getPlayer(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 0.5f + (float) v / (float) range);
        Rewards.changeCoins(e.getPlayer(), v * v, "fishing");
      } else
      {
        e.getPlayer().sendActionBar(Component.text("Nothing..."));
      }
      break;
    default:
      break;
    }
  }

  @EventHandler
  public void onItemPickUp(EntityPickupItemEvent e)
  {
    if (e.getEntityType() == EntityType.PLAYER)
    {
      final var player = (Player) e.getEntity();
      if (Cosmetics.Blocks.isMaterial(Cosmetics.Blocks.fall_materials, e.getItem()))
      {
        e.getItem().setItemStack(
            new ItemStack(Cosmetics.Blocks.fall_materials[Database.getCachedIntStat(player, "cos_blk").orElse(0)],
                e.getItem().getItemStack().getAmount()));
      } else if (Cosmetics.Blocks.isMaterial(Cosmetics.Blocks.solid_materials, e.getItem()))
      {
        e.getItem().setItemStack(
            new ItemStack(Cosmetics.Blocks.solid_materials[Database.getCachedIntStat(player, "cos_blk").orElse(0)],
                e.getItem().getItemStack().getAmount()));
      }
    }
  }
}
