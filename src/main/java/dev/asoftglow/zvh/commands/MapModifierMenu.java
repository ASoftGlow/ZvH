package dev.asoftglow.zvh.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import dev.asoftglow.zvh.Game;
import dev.asoftglow.zvh.MapControl;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.SelectButton;
import dev.asoftglow.zvh.util.SelectionMenu;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.janboerman.guilib.api.ItemBuilder;

public abstract class MapModifierMenu
{
  public interface MapModifier
  {
    public String getName();

    public String getDescription();

    public String getType();

    public Material getIcon();
  }

  private final static SelectButton<ZvH> closeBtn = new SelectButton<ZvH>(
      (new ItemBuilder(Material.BARRIER)).flags(ItemFlag.HIDE_ENCHANTS).name("§r§5Leave").build(), e -> {
        e.getWhoClicked().addScoreboardTag("clicked");
        Game.leave((Player) e.getWhoClicked());
        return true;
      });
  private static final Set<SelectButton<ZvH>> modifiationBtns = new HashSet<>();

  static
  {
    for (MapModifier modifier : MapControl.features)
    {
      if (modifier.getName() == null)
        continue;
      var item = new ItemBuilder(modifier.getIcon()).name("§r§f" + modifier.getName())
          .lore("§r§8" + modifier.getDescription(), modifier.getType()).build();

      modifiationBtns.add(new SelectButton<ZvH>(item, e -> {
        item.displayName().color(NamedTextColor.BLACK);
        e.getClickedInventory().setItem(e.getSlot(), item);
        return false;
      }));
    }
  }

  public static void showTo(Player player)
  {
    var menu = new SelectionMenu<>(ZvH.singleton, 9, "Vote!");
    // var menu = new MenuHolder<>(ZvH.singleton, 9, "Vote for a feature:");
    var it = modifiationBtns.iterator();
    var i = 0;
    while (it.hasNext())
    {
      menu.setButton(i++, it.next());
    }
    menu.setButton(8, closeBtn);
    player.openInventory(menu.getInventory());
  }
}
