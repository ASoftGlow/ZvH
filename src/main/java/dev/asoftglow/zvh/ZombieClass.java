package dev.asoftglow.zvh;

import org.bukkit.potion.PotionEffect;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import lombok.Setter;

public class ZombieClass implements SpeciesClass
{
  @NotNull
  public final String name;
  @NotNull
  public final Material icon;
  public final int price;
  @NotNull
  public final PotionEffect[] effects;
  @Setter
  public ItemStack[] items = new ItemStack[0];

  public ZombieClass(@NotNull String name, @NotNull Material icon, int price, ItemStack[] items, PotionEffect[] effects)
  {
    this.name = name;
    this.icon = icon;
    this.price = price;
    if (items != null)
      this.items = items;
    this.effects = effects == null ? new PotionEffect[]
    {} : effects;
  }

  public void giveTo(@NotNull Player player)
  {
    player.getInventory().setContents(items);
    for (var e : effects)
    {
      player.addPotionEffect(e);
    }
  }
}
