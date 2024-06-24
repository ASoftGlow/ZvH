package dev.asoftglow.zvh;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

public class GuardListener implements Listener
{
  @EventHandler
  public void onDrop(PlayerDropItemEvent e)
  {
    if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
      e.setCancelled(true);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent e)
  {
    if (Game.isPlaying(e.getPlayer()) && e.getPlayer().getGameMode() == GameMode.SURVIVAL)
    {
      final var outside_map = !MapControl.current_size.bounds().contains(e.getBlock());
      if (outside_map || e.getBlock().getY() == MapControl.current_size.bounds().y)
      {
        e.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onSlimeSplit(SlimeSplitEvent e)
  {
    e.setCancelled(true);
  }

  private static final Set<Material> explodeWhitelist = new HashSet<>();
  static
  {
    explodeWhitelist.add(Material.LADDER);
    explodeWhitelist.add(Material.COBWEB);
    explodeWhitelist.addAll(Arrays.asList(CustomItems.fall_blocks));
    explodeWhitelist.addAll(Arrays.asList(CustomItems.solid_blocks));
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent e)
  {
    var fling = new ArrayList<Block>();
    var destroyed = e.blockList();
    var it = destroyed.iterator();
    while (it.hasNext())
    {
      var block = it.next();
      if (!explodeWhitelist.contains(block.getType()))
        it.remove();
      else if (ThreadLocalRandom.current().nextBoolean() && block.isSolid())
      {
        fling.add(block);
      }
    }

    for (var b : fling)
    {
      var fb = e.getLocation().getWorld().spawn(b.getLocation(), FallingBlock.class);
      fb.setBlockData(b.getBlockData());
      fb.setBlockState(b.getState());
      var v2 = b.getLocation().toVector();
      v2.setY(v2.getY() + 2);
      var v = v2.subtract(e.getLocation().toVector());
      if (v.getX() == 0)
        v.setX(1);
      if (v.getY() == 0)
        v.setY(1);
      if (v.getZ() == 0)
        v.setZ(1);
      fb.setVelocity(new Vector(0.5, 0.5, 0.5).divide(v));
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e)
  {
    if (e.getPlayer().getGameMode() != GameMode.ADVENTURE || e.getAction() != Action.RIGHT_CLICK_BLOCK)
      return;
    if (e.getClickedBlock().getType() == Material.DARK_OAK_BUTTON)
      return;
    if (e.hasItem() && e.getItem().getType() == Material.FISHING_ROD)
      return;
    e.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onFallingBlockSettle(EntityChangeBlockEvent e)
  {
    if (e.getEntityType() == EntityType.FALLING_BLOCK)
    {
      if (MapControl.current_size != null
          && (e.getBlock().getLocation().toVector().distanceSquared(MapControl.current_size.zombieSpawn()) < 9
              || !MapControl.current_size.bounds().contains(e.getBlock())))
        e.setCancelled(true);
    }
  }

  @EventHandler
  public void onIfCanPlace(BlockCanBuildEvent e)
  {
    if (Game.isPlaying(e.getPlayer()))
    {
      final var near_zombie_spawn = MapControl.current_size.zombieSpawn().distanceSquared(
          e.getBlock().getLocation().toVector().setY(MapControl.current_size.zombieSpawn().getY())) < 9;
      final var outside_map = !MapControl.current_size.bounds().contains(e.getBlock());
      if (near_zombie_spawn || outside_map)
        e.setBuildable(false);
    }
  }

  @EventHandler
  public void onDamaged(EntityDamageEvent e)
  {
    final boolean cause_is_artifial = e.getCause() == DamageCause.VOID || e.getCause() == DamageCause.WORLD_BORDER
        || e.getCause() == DamageCause.CUSTOM;
    if (e.getEntity() instanceof Player
        && !(Game.isPlaying((Player) e.getEntity()) && Game.getState() == Game.State.PLAYING) && !cause_is_artifial)
    {
      e.setDamage(0);
    }
  }

  @EventHandler
  public void onPlaced(BlockPlaceEvent e)
  {
    if (e.getPlayer() == null)
    {
      if (e.getBlock().getType() == Material.GRAVEL)
      {
        e.getBlock()
            .setType(CustomItems.fall_blocks[ThreadLocalRandom.current().nextInt(1, CustomItems.fall_blocks.length)]);

      } else if (e.getBlock().getType() == Material.LIGHT_GRAY_WOOL)
      {
        e.getBlock()
            .setType(CustomItems.solid_blocks[ThreadLocalRandom.current().nextInt(1, CustomItems.solid_blocks.length)]);
      }
    }
  }
}
