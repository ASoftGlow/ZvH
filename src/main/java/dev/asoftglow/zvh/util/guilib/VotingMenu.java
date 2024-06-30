package dev.asoftglow.zvh.util.guilib;

import java.lang.Runnable;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import org.apache.commons.lang3.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.Utils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import xyz.janboerman.guilib.api.GuiInventoryHolder;

public class VotingMenu<P extends Plugin>
{
  private static final Sound click = Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.5f, 1f);
  private final ItemStack[] buttons;
  private final int[] scores;
  private int sum = 0;
  private final P plugin;
  private final String title;
  private final Consumer<Integer> callback;
  public Runnable callback2; // FIXME
  private int inital_time = 0;
  private MutableInt time = null;
  private Collection<Player> players;
  boolean canLeave = false;

  public Collection<Player> getPlayers()
  {
    players.removeIf(p -> !p.isConnected());
    return players;
  }

  public VotingMenu(P plugin, int size, String title, Consumer<Integer> callback, int time)
  {
    this(plugin, size, title, callback);
    inital_time = time;
    this.time = new MutableInt(inital_time);
  }

  public VotingMenu(P plugin, int size, String title, Consumer<Integer> callback)
  {
    this.plugin = plugin;
    this.title = title;
    this.callback = callback;
    buttons = new ItemStack[size];
    scores = new int[size];
    reset();
  }

  public void reset()
  {
    canLeave = false;
    Arrays.fill(scores, 0);
    if (isTimed())
    {
      time = new MutableInt(inital_time);
    }
  }

  public boolean isTimed()
  {
    return inital_time > 0;
  }

  public void setButton(int slot, ItemStack button)
  {
    if (button != null)
    {
      var meta = button.getItemMeta();
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      button.setItemMeta(meta);
    }

    buttons[slot] = button;
  }

  public void showTo(Collection<Player> players)
  {
    this.players = players;
    for (var p : players)
    {
      p.openInventory(new SelectionMenu().getInventory());
    }

    if (isTimed())
    {
      Bukkit.getScheduler().runTaskTimer(plugin, t -> {
        if (time.intValue() == 0)
        {
          end();
          t.cancel();
          return;
        }
        for (var p : getPlayers())
        {
          var inv = p.getOpenInventory();
          // not correct inventory
          if (inv.getTopInventory().getSize() != scores.length)
            return;
          inv.setTitle(inv.getOriginalTitle() + " (" + time.toString() + ")");
        }
        time.subtract(1);
      }, 0, 20);
    }
  }

  private void end()
  {
    canLeave = true;
    for (var p : getPlayers())
    {
      p.closeInventory();
    }
    var winner = Utils.getMaxI(scores);
    callback.accept(scores[winner] == 0 ? null : Integer.valueOf(winner));
    callback2.run();
  }

  private void updateScore(int slot)
  {
    for (var p : getPlayers())
    {
      var inv = p.getOpenInventory();
      // not correct inventory
      if (inv.getTopInventory().getSize() != scores.length)
        return;

      inv.getItem(slot).setAmount(scores[slot] + 1);
    }
  }

  class SelectionMenu extends GuiInventoryHolder<P>
  {
    private int selected = -1;
    private final ItemStack items[] = new ItemStack[scores.length];

    public SelectionMenu()
    {
      super(plugin, scores.length, title);
      for (int slot = 0; slot < buttons.length; slot++)
      {
        if (buttons[slot] != null)
        {
          items[slot] = buttons[slot].clone();
          this.getInventory().setItem(slot, items[slot]);
        }
      }
    }

    public void onClick(InventoryClickEvent event)
    {
      Inventory clickedInventory = getClickedInventory(event);
      if (clickedInventory != null && clickedInventory == this.getInventory())
      {
        var slot = event.getRawSlot();
        if (buttons[slot] == null || slot == selected)
          return;
        clickedInventory.getViewers().get(0).playSound(click);
        selectButton(slot);
      }
    }

    void selectButton(int slot)
    {
      if (selected > 0)
      {
        unselectButton();
      } else
      {
        sum++;
      }
      selected = slot;
      items[slot].addUnsafeEnchantment(Enchantment.LUCK, 1);
      getInventory().setItem(slot, items[slot]);
      scores[slot]++;

      if (!isTimed() && sum == getPlayers().size())
      {
        end();
      }
      updateScore(slot);
    }

    void unselectButton()
    {
      items[selected].removeEnchantment(Enchantment.LUCK);
      getInventory().setItem(selected, items[selected]);
      scores[selected]--;
      updateScore(selected);
    }

    @Override
    public void onClose(InventoryCloseEvent event)
    {
      if (!canLeave)
      {
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> event.getPlayer().openInventory(event.getView()));
      }
    }
  }
}