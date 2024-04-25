package dev.asoftglow.zvh.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;

import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.*;
import dev.asoftglow.zvh.Game;
import dev.asoftglow.zvh.ZClass;
import dev.asoftglow.zvh.ZClassManager;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.SelectButton;

public class ClassSelectionMenu implements Listener {
  private static MenuHolder<ZvH> menu;
  private static InventoryView lastView;

  private boolean SelectionHandler(Player player, ZClass zClass) {
    if (player.getGameMode() == GameMode.CREATIVE) {
      player.addScoreboardTag("clicked");
      zClass.give(player);
      return true;
    }
    if (ZvH.coins.getScore(player).getScore() >= zClass.price) {
      ZvH.changeCoins(player, -zClass.price);
      player.addScoreboardTag("clicked");
      zClass.give(player);
      return true;
    }
    return false;
  }

  public ClassSelectionMenu(ZvH zvh) {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new SelectButton<ZvH>((new ItemBuilder(Material.BARRIER)).name("§r§6Leave").build(), e -> {
      e.getWhoClicked().addScoreboardTag("clicked");
      Game.leave((Player) e.getWhoClicked());
      return true;
    }));

    int i = 0;
    for (var zClass : ZClassManager.zClasses.values()) {
      var item = new ItemBuilder(zClass.icon).name("§r§f" + zClass.name);
      if (zClass.price > 0)
        item = item.lore("Costs " + zClass.price);
      menu.setButton(i++,
          new SelectButton<ZvH>(item.build(), e -> SelectionHandler((Player) e.getWhoClicked(), zClass)));
    }
  }

  public static void showTo(Player player) {
    lastView = player.openInventory(menu.getInventory());
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (e.getPlayer() instanceof Player && lastView == e.getView()) {
      if (e.getPlayer().getScoreboardTags().contains("clicked"))
        e.getPlayer().removeScoreboardTag("clicked");
      else
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> showTo((Player) e.getPlayer()));
    }
  }
}
