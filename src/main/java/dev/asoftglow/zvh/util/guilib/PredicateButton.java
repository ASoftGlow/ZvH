package dev.asoftglow.zvh.util.guilib;

import java.util.function.BiPredicate;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public class PredicateButton<MH extends MenuHolder<?>> implements MenuButton<MH>
{
  protected ItemButton<MH> delegate;
  private final BiPredicate<MH, InventoryClickEvent> predicate;

  public PredicateButton(ItemButton<MH> delegate, BiPredicate<MH, InventoryClickEvent> predicate)
  {
    this.delegate = delegate;
    this.predicate = predicate;
  }

  public void onClick(MH menuHolder, InventoryClickEvent event)
  {
    if (this.getPredicate().test(menuHolder, event))
    {
      this.getDelegate().onClick(menuHolder, event);
    } else
    {
      Bukkit.getScheduler().runTask(menuHolder.getPlugin(), event.getView()::close);
    }
  }

  protected MenuButton<MH> getDelegate()
  {
    return this.delegate;
  }

  protected BiPredicate<MH, InventoryClickEvent> getPredicate()
  {
    return this.predicate;
  }

  public void setIcon(ItemStack item)
  {
    this.delegate.setIcon(item);
  }

  public ItemStack getIcon()
  {
    return this.delegate.getIcon();
  }

  public boolean onAdd(MH menuHolder, int slot)
  {
    return this.getDelegate().onAdd(menuHolder, slot);
  }

  public boolean onRemove(MH menuHolder, int slot)
  {
    return this.getDelegate().onRemove(menuHolder, slot);
  }
}