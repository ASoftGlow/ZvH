package dev.asoftglow.zvh;

import java.util.function.BiPredicate;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.asoftglow.zvh.util.Util;
import dev.asoftglow.zvh.util.guilib.CloseButton;
import dev.asoftglow.zvh.util.guilib.PredicateButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import xyz.janboerman.guilib.api.menu.ItemButton;

public class ShopMenu extends MenuHolder<ZvH>
{
  private record ShopItem(ItemStack item, int price)
  {
  }

  private final static ShopItem[] zombie_items = new ShopItem[]
  { new ShopItem(
      new ItemBuilder(Material.GUNPOWDER).name("Explosive Powder").lore("Right click to launch an explosive").build(),
      12), new ShopItem(new ItemStack(Material.SHEARS), 8), new ShopItem(new ItemStack(Material.STONE_SHOVEL), 8),
      new ShopItem(new ItemStack(Material.LIGHT_GRAY_WOOL, 5), 5), new ShopItem(CustomItems.tracker, 4) },
      human_items = new ShopItem[]
      { new ShopItem(new ItemStack(Material.ARROW, 4), 9), new ShopItem(new ItemStack(Material.SHEARS), 8),
          new ShopItem(new ItemStack(Material.GOLDEN_APPLE), 40),
          new ShopItem(new ItemStack(Material.LIGHT_GRAY_WOOL, 5), 5),
          new ShopItem(new ItemBuilder(Material.NETHERRACK).name("Unbreakable Flesh")
              .lore("Absorbs explosions").amount(2).build(), 10),
          new ShopItem(new ItemStack(Material.GLASS, 2), 7),
          new ShopItem(new ItemBuilder(Material.SHIELD).damage(296).build(), 18),
          new ShopItem(new ItemStack(Material.FISHING_ROD, 1, (short) 44), 15) };

  private static final BiPredicate<ShopMenu, InventoryClickEvent> bp = (h, e) -> {
    var player = (Player) e.getWhoClicked();
    var item = h.items[e.getSlot()];
    if (h.coins >= item.price)
    {
      if (player.getGameMode() == GameMode.SURVIVAL)
      {
        Rewards.changeCoins(player, -item.price, "shopping");
        h.coins -= item.price;
      }

      ItemStack is = item.item;
      if (item.item.getType() == Material.GRAVEL)
      {
        is = new ItemStack(CustomItems.fall_blocks[0], is.getAmount());

      } else if (item.item.getType() == Material.LIGHT_GRAY_WOOL)
      {
        is = new ItemStack(CustomItems.solid_blocks[0], is.getAmount());
      }

      player.getInventory().addItem(is);

      h.updateItems();
      Util.playSound(player, Sound.UI_BUTTON_CLICK, 0.9f, 1f);

    } else
    {
      Util.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.9f, 1f);
    }
    return true;
  };

  private static void verifyDisplayName(ItemStack item, ItemMeta meta)
  {
    if (meta.displayName() == null)
    {
      var builder = new StringBuilder();
      for (String string : item.getType().name().split("_"))
      {
        builder.append(' ');
        builder.append(string.substring(0, 1).toUpperCase());
        builder.append(string.substring(1).toLowerCase());
      }
      builder.deleteCharAt(0);
      meta.displayName(Component.text(builder.toString()));
      item.setItemMeta(meta);
    }
  }

  private int coins = 0;
  private final ShopItem[] items;

  private void updateItems()
  {
    for (int i = 0; i < 8; i++)
    {
      var _btn = getButton(i);
      if (_btn == null)
        break;
      var btn = (PredicateButton<?>) _btn;
      var icon = btn.getIcon();
      var meta = icon.getItemMeta();
      boolean affordable = items[i].price <= coins;
      meta.displayName(meta.displayName().style(affordable ? Styles.affordable_style : Styles.too_expensive_style));
      icon.setItemMeta(meta);
      btn.setIcon(icon);
    }
  }

  private ShopMenu(Player player, boolean isHuman)
  {
    super(ZvH.singleton, 9, isHuman ? "Humans' Shop" : "Zombies' Shop");

    items = isHuman ? human_items : zombie_items;
    coins = Database.getCachedIntStat(player, "coins").orElse(0);

    for (var i = 0; i < items.length; i++)
    {
      var item = items[i].item.clone();
      var meta = item.getItemMeta();
      boolean isAffordable = items[i].price <= coins;

      verifyDisplayName(item, meta);
      meta.displayName(meta.displayName().style(isAffordable ? Styles.affordable_style : Styles.too_expensive_style));
      Util.addLore(meta, Component.text("Costs ", NamedTextColor.GRAY)
          .append(Component.text(items[i].price, NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
      item.setItemMeta(meta);

      setButton(i, new PredicateButton<>(new ItemButton<>(item), bp));
    }
    setButton(8, new CloseButton<>());

    player.openInventory(getInventory());
  }

  public static void showTo(Player player)
  {
    new ShopMenu(player, Game.humans.contains(player));
  }

  public static void handleCommand(Player player)
  {
    if ((!Game.isPlaying(player) || Game.getState() != Game.State.PLAYING) && !player.isOp())
    {
      player.sendMessage("! Must be in-game");
      return;
    }

    showTo(player);
  }
}
