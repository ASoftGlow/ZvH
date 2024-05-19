package dev.asoftglow.zvh;

import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.CartographyInventory;

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
    if (Game.isActive() && e.getPlayer().getGameMode().equals(GameMode.SURVIVAL))
      if (e.getBlock().getLocation().getBlockY() == MapControl.current_size.bounds().y)
      {
        e.setCancelled(true);
      }
  }

  @EventHandler
  public void onSlimeSplit(SlimeSplitEvent e)
  {
    e.setCancelled(true);
  }

  private static final Set<Material> explodeWhitelist = Set.of(Material.GRAVEL, Material.LIGHT_GRAY_WOOL,
      Material.LADDER, Material.COBWEB);

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
  public void onSignChanged(SignChangeEvent e)
  {
    if (e.getPlayer().getGameMode() == GameMode.ADVENTURE)
      e.setCancelled(true);
  }

  @EventHandler
  public void onContainerOpened(InventoryOpenEvent e)
  {
    if (e.getPlayer().getGameMode() == GameMode.ADVENTURE
        && (e.getInventory().getHolder() instanceof Container || e.getInventory() instanceof CartographyInventory))
      e.setCancelled(true);
  }
}
