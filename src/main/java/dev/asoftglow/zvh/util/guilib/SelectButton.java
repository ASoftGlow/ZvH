package dev.asoftglow.zvh.util.guilib;

import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

/**
 * A MenuButton that closes the Inventory and calls a callback when clicked.
 */
public class SelectButton<P extends Plugin> extends ItemButton<MenuHolder<P>>
{
  private final Predicate<InventoryClickEvent> callback;

  /**
   * Creates the select button with the custom icon and callback.
   * 
   * @param icon the icon
   * @param callback a callback accepting an InventoryClickEvent
   */
  public SelectButton(ItemStack icon, Predicate<InventoryClickEvent> callback)
  {
    super(icon);
    this.callback = callback;
  }

  /**
   * Closes the inventory after one tick and calls callback.
   * 
   * @param holder the MenuHolder
   * @param event the InventoryClickEvent
   */
  @Override
  public final void onClick(MenuHolder<P> holder, InventoryClickEvent event)
  {
    if (callback.test(event))
      Bukkit.getScheduler().runTask(holder.getPlugin(), event.getView()::close);
  }
}