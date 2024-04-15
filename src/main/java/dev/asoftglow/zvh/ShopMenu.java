package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.CloseButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import xyz.janboerman.guilib.api.menu.ItemButton;

public abstract class ShopMenu {
  private final static ShopItem[] zombie_items = new ShopItem[] {
      new ShopItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), 100)
  },
      human_items = new ShopItem[] {
          new ShopItem(new ItemStack(Material.ARROW, 5), 5),
          new ShopItem(new ItemStack(Material.SHEARS), 30),
          new ShopItem(new ItemStack(Material.STONE_SHOVEL), 30)
      };

  private static MenuHolder<ZvH> zombies_menu, humans_menu;

  private record ShopItem(ItemStack item, int price) {
  }

  public static void init() {
    zombies_menu = new MenuHolder<>(ZvH.singleton, 9, "Zombies' Shop");
    humans_menu = new MenuHolder<ZvH>(ZvH.singleton, 9, "Humans' Shop");

    zombies_menu.setButton(8, new CloseButton<ZvH>(Material.BARRIER));
    humans_menu.setButton(8, new CloseButton<ZvH>(Material.BARRIER));

    for (var i = 0; i < zombie_items.length; i++) {

      zombies_menu.setButton(i, new ItemButton<>(
          new ItemBuilder(zombie_items[i].item).lore("Costs " + zombie_items[i].price).build()));
    }
    for (var i = 0; i < human_items.length; i++) {
      humans_menu.setButton(i, new ItemButton<>(
          new ItemBuilder(human_items[i].item).lore("Costs " + human_items[i].price).build()));
    }
  }

  public static void showTo(Player player) {
    player.openInventory(ZvH.humansTeam.hasPlayer(player)
        ? humans_menu.getInventory()
        : zombies_menu.getInventory());
  }

  public static void handleCommand(Player player) {
    if (!Game.playerIsPlaying(player)) {
      player.sendMessage("! Must be in-game");
      return;
    }

    showTo(player);
  }
}
