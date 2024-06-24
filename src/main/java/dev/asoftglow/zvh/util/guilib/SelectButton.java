package dev.asoftglow.zvh.util.guilib;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

/**
 * A MenuButton that closes the Inventory and calls a callback when clicked.
 */
public class SelectButton<MH extends MenuHolder<?>> implements MenuButton<MH>
{
  protected ItemStack stack;
  private final WeakHashMap<MH, Set<Integer>> inventoriesContainingMe = new WeakHashMap<>();

  protected SelectButton()
  {
  }

  public SelectButton(ItemStack stack)
  {
    this.stack = stack;
  }

  public final ItemStack getIcon()
  {
    return stack;
  }

  public final void setIcon(ItemStack icon)
  {
    this.stack = icon;
  }

  public final boolean onAdd(MH menuHolder, int slot)
  {
    return this.inventoriesContainingMe.computeIfAbsent(menuHolder, mh -> new HashSet<>()).add(slot);
  }

  public final boolean onRemove(MH menuHolder, int slot)
  {
    Set<Integer> slots = this.inventoriesContainingMe.get(menuHolder);
    if (slots != null)
    {
      boolean result = slots.remove(slot);
      if (slots.isEmpty())
      {
        this.inventoriesContainingMe.remove(menuHolder);
      }

      return result;
    } else
    {
      return true;
    }
  }

  /**
   * Closes the inventory after one tick and calls callback.
   * 
   * @param holder the MenuHolder
   * @param event the InventoryClickEvent
   */
  public final void onClick(MH holder, InventoryClickEvent event)
  {

  }
}