package dev.asoftglow.zvh.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import dev.asoftglow.zvh.Game;
import dev.asoftglow.zvh.MapControl;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.SelectButton;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.janboerman.guilib.api.ItemBuilder;
//import xyz.janboerman.guilib.api.menu.;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public abstract class MapModifierMenu
{
  private final static SelectButton<ZvH> closeBtn = new SelectButton<ZvH>(
      (new ItemBuilder(Material.BARRIER)).flags(ItemFlag.HIDE_ENCHANTS).name("§r§5Leave").build(), e -> {
        e.getWhoClicked().addScoreboardTag("clicked");
        Game.leave((Player) e.getWhoClicked());
        return true;
      });
  private static final Set<SelectButton<ZvH>> featureBtns = new HashSet<>();

  static
  {
    for (var feature : MapControl.features)
    {
      var item = new ItemBuilder(feature.icon).name("§r§f" + feature.name).lore("§r§8" + feature.description).build();

      featureBtns.add(new SelectButton<ZvH>(item, e -> {
        item.addUnsafeEnchantment(Enchantment.LUCK, 1);
        item.displayName().color(NamedTextColor.BLACK);
        e.getClickedInventory().setItem(e.getSlot(), item);
        return false;
      }));
    }
  }

  public static void showTo(Player player)
  {
    var menu = new MenuHolder<>(ZvH.singleton, 9, "Vote for a feature:");
    var it = featureBtns.iterator();
    var i = 0;
    while (it.hasNext())
    {
      menu.setButton(i++, it.next());
    }
    menu.setButton(8, closeBtn);
    player.openInventory(menu.getInventory());
  }
}
