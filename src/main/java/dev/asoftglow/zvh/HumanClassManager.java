package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class HumanClassManager
{
  public static void giveTo(Player player)
  {
    var inv = player.getInventory();
    inv.addItem(new ItemStack(player.getName().equals("AthenaViolet") ? Material.IRON_SWORD : Material.STONE_SWORD));
    inv.addItem(new ItemStack(Material.BOW));
    // y=\max\left(\operatorname{floor}\left(\frac{55}{\operatorname{floor}\left(x\right)+9}\right),2\right)
    inv.addItem(new ItemStack(Material.ARROW, Math.max(2, 55 / (Game.playing.size() + 9))));
    inv.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
    inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
    inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
  }
}
