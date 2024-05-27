package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import xyz.janboerman.guilib.api.ItemBuilder;

public abstract class CustomItems
{
  public static final ItemStack tracker = new ItemBuilder(Material.COMPASS).name("§rTracker")
      .lore("Points to humans by detecting their heat signature").build();
  public static final ItemStack shop_open = new ItemBuilder(Material.BOOK).name("§rOpen Shop").lore("Right click to use")
      .build();
  public static final ItemStack spec_leave = new ItemBuilder(Material.BARRIER).name("§r§6Click to leave").build();
}
