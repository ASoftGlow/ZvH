package dev.asoftglow.zvh.util;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import xyz.janboerman.guilib.api.GuiInventoryHolder;
import xyz.janboerman.guilib.api.menu.MenuButton;

public class SelectionMenu<P extends Plugin> extends GuiInventoryHolder<P>
{
  private static final Sound click = Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.5f, 1f);
  private final MenuButton<?>[] buttons;
  private MenuButton<?> selected = null;

  public SelectionMenu(P plugin, int size, String title)
  {
    super(plugin, size, title);
    buttons = new MenuButton[size];
  }

  public void setButton(int slot, MenuButton<?> button)
  {
    buttons[slot] = button;
    this.getInventory().setItem(slot, button.getIcon());
  }

  public void onClick(InventoryClickEvent event)
  {
    Inventory clickedInventory = getClickedInventory(event);
    if (clickedInventory != null && clickedInventory == this.getInventory())
    {
      var button = buttons[event.getRawSlot()];
      if (button == null)
        return;
      clickedInventory.getViewers().get(0).playSound(click);
      if (button == selected)
        return;
      selectButton(button);
    }
  }

  void selectButton(MenuButton<?> button)
  {
    if (selected != null)
      unselectButton(selected);
    selected = button;
    button.getIcon().addUnsafeEnchantment(Enchantment.LUCK, 1);
  }

  void unselectButton(MenuButton<?> button)
  {
    button.getIcon().removeEnchantment(Enchantment.LUCK);
  }
}