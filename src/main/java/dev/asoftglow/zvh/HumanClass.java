package dev.asoftglow.zvh;

import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import lombok.Setter;

public class HumanClass implements SpeciesClass
{
  @NotNull
  public final PotionEffect[] effects;
  @Setter
  public ItemStack[] items = new ItemStack[0];

  public HumanClass(ItemStack[] items, PotionEffect[] effects)
  {
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
