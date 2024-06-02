package dev.asoftglow.zvh.util.guilib;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;

public class PredicateButton<MH extends MenuHolder<?>> implements MenuButton<MH>
{
  protected ItemButton<MH> delegate;
  private final BiPredicate<MH, InventoryClickEvent> predicate;
  private final BiConsumer<MH, InventoryClickEvent> predicateFailedCallback;

  public PredicateButton(ItemButton<MH> delegate, BiPredicate<MH, InventoryClickEvent> predicate)
  {
    this(delegate, predicate, null);
  }

  public PredicateButton(ItemButton<MH> delegate, BiPredicate<MH, InventoryClickEvent> predicate,
      BiConsumer<MH, InventoryClickEvent> predicateFailedCallback)
  {
    this.delegate = delegate;
    this.predicate = predicate;
    this.predicateFailedCallback = predicateFailedCallback;
  }

  public void onClick(MH menuHolder, InventoryClickEvent event)
  {
    if (this.getPredicate().test(menuHolder, event))
    {
      this.getDelegate().onClick(menuHolder, event);
    } else
    {
      this.getPredicateFailedCallback().ifPresent((callback) -> {
        callback.accept(menuHolder, event);
      });
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

  protected Optional<BiConsumer<MH, InventoryClickEvent>> getPredicateFailedCallback()
  {
    return Optional.ofNullable(this.predicateFailedCallback);
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