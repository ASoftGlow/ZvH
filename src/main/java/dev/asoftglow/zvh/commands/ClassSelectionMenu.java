package dev.asoftglow.zvh.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

public class ClassSelectionMenu implements CommandExecutor, Listener {
  private static MenuHolder<ZvH> menu;
  private static InventoryView lastView;

  private void SelectionHandler(Player player, ZClass zClass) {
    zClass.give(player);
    player.addScoreboardTag("test");
  }

  public ClassSelectionMenu(ZvH zvh) {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new SelectButton<ZvH>((new ItemBuilder(Material.BARRIER)).name("§r§6Leave").build(), e -> {
      e.getWhoClicked().addScoreboardTag("test");
      Game.leave((Player) e.getWhoClicked());
    }));

    int i = 0;
    for (var zClass : ZClassManager.zClasses.values()) {
      var item = new ItemBuilder(zClass.icon).name("§r§f" + zClass.name).lore("Costs " + zClass.price).build();
      menu.setButton(i++, new SelectButton<ZvH>(item, e -> SelectionHandler((Player) e.getWhoClicked(), zClass)));
    }
  }

  public static void showTo(Player player) {
    lastView = player.openInventory(menu.getInventory());
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label,
      String[] args) {
    if (sender instanceof Player) {
      ClassSelectionMenu.showTo((Player) sender);
    }
    return true;
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (e.getPlayer() instanceof Player && lastView == e.getView()) {
      if (e.getPlayer().getScoreboardTags().contains("test"))
        e.getPlayer().removeScoreboardTag("test");
      else
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> showTo((Player) e.getPlayer()));
    }
  }
}
