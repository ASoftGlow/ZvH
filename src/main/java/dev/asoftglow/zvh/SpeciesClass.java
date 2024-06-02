package dev.asoftglow.zvh;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface SpeciesClass
{
  public void setItems(ItemStack[] items);
  public void giveTo(@NotNull Player player);
}
