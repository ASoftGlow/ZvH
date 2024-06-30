package dev.asoftglow.zvh.util.guilib;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dev.asoftglow.zvh.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public class CloseButton<P extends Plugin> extends ItemButton<MenuHolder<P>>
{
  static final ItemStack icon = new ItemStack(Material.BARRIER);
  static
  {
    var meta = icon.getItemMeta();
    meta.displayName(Component.text("Close").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
    icon.setItemMeta(meta);
  }

  public CloseButton()
  {
    super(icon);
  }

  public final void onClick(MenuHolder<P> holder, InventoryClickEvent event)
  {
    holder.getPlugin().getServer().getScheduler().runTask(holder.getPlugin(), event.getView()::close);
    Utils.playSound(event.getWhoClicked(), Sound.UI_BUTTON_CLICK, 0.9f, 1f);
  }
}
