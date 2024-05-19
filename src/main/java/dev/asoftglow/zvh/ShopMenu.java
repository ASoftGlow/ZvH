package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.asoftglow.zvh.util.Util;
import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.CloseButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import xyz.janboerman.guilib.api.menu.PredicateButton;
import xyz.janboerman.guilib.api.menu.ItemButton;

public abstract class ShopMenu
{
  public final static ItemStack tracker = new ItemBuilder(Material.COMPASS).name("§rTracker")
      .lore("Points to humans by detecting their heat signature").build();
  private final static ShopItem[] zombie_items = new ShopItem[]
  { new ShopItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), 100),
      new ShopItem(new ItemBuilder(Material.GUNPOWDER).name("Explosive Powder").lore("Right click to throw").build(),
          10),
      new ShopItem(new ItemStack(Material.SHEARS), 15), new ShopItem(new ItemStack(Material.STONE_SHOVEL), 15),
      new ShopItem(new ItemStack(Material.LIGHT_GRAY_WOOL, 5), 5), new ShopItem(tracker, 10) },
      human_items = new ShopItem[]
      { new ShopItem(new ItemStack(Material.ARROW, 3), 7), new ShopItem(new ItemStack(Material.SHEARS), 15),
          new ShopItem(new ItemStack(Material.GOLDEN_APPLE), 40),
          new ShopItem(new ItemStack(Material.LIGHT_GRAY_WOOL, 5), 5),
          new ShopItem(new ItemBuilder(Material.SHIELD).damage(296).build(), 15) };

  private final static MenuHolder<ZvH> zombies_menu, humans_menu;

  private record ShopItem(ItemStack item, int price)
  {
  }

  static
  {
    zombies_menu = new MenuHolder<>(ZvH.singleton, 9, "Zombies' Shop");
    humans_menu = new MenuHolder<ZvH>(ZvH.singleton, 9, "Humans' Shop");

    zombies_menu.setButton(8, new CloseButton<ZvH>(Material.BARRIER));
    humans_menu.setButton(8, new CloseButton<ZvH>(Material.BARRIER));

    for (var i = 0; i < zombie_items.length; i++)
    {
      var btn = new PredicateButton<>(
          new ItemButton<>(new ItemBuilder(zombie_items[i].item).addLore("§r§6Costs " + zombie_items[i].price).build()),
          (h, p) -> {
            var player = (Player) p.getWhoClicked();
            if (ZvH.coins.getScore(player).getScore() >= zombie_items[p.getSlot()].price)
            {
              ZvH.changeCoins(player, -zombie_items[p.getSlot()].price, "shopping");
              player.getInventory().addItem(zombie_items[p.getSlot()].item);
              Util.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return false;
          });
      zombies_menu.setButton(i, btn);
    }
    for (var i = 0; i < human_items.length; i++)
    {
      var btn = new PredicateButton<>(
          new ItemButton<>(new ItemBuilder(human_items[i].item).addLore("§r§6Costs " + human_items[i].price).build()),
          (h, p) -> {
            var player = (Player) p.getWhoClicked();
            if (ZvH.coins.getScore(player).getScore() >= human_items[p.getSlot()].price)
            {
              ZvH.changeCoins(player, -human_items[p.getSlot()].price, "shopping");
              player.getInventory().addItem(human_items[p.getSlot()].item);
              Util.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return false;
          });
      humans_menu.setButton(i, btn);
    }
  }

  public static void showTo(Player player)
  {
    player.openInventory(ZvH.humansTeam.hasPlayer(player) ? humans_menu.getInventory() : zombies_menu.getInventory());
  }

  public static void handleCommand(Player player)
  {
    if (!Game.isPlaying(player))
    {
      player.sendMessage("! Must be in-game");
      return;
    }

    showTo(player);
  }
}
