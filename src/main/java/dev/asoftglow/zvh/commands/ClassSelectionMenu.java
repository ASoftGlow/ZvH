package dev.asoftglow.zvh.commands;

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
import dev.asoftglow.zvh.ZClass;
import dev.asoftglow.zvh.ZClassManager;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.SelectButton;

public class ClassSelectionMenu implements CommandExecutor, Listener {
  private static MenuHolder<ZvH> menu;
  private static InventoryView lastView;

  private void SelectionHandler(Player player, ZClass zClass) {
    player.sendMessage(zClass.name);
    zClass.give(player);
    player.addScoreboardTag("test");
  }

  public ClassSelectionMenu(ZvH zvh) {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new CloseButton<>(Material.BARRIER, "§rLeave"));

    int i = 0;
    for (var zClass : ZClassManager.zClasses.values()) {
      var item = new ItemBuilder(zClass.icon).name("§r" + zClass.name).lore("Costs " + zClass.price).build();
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
    if (e.getPlayer() instanceof Player && lastView == e.getView()
        && e.getPlayer().getScoreboardTags().contains("test"))
      e.getPlayer().removeScoreboardTag("test");
    //Bukkit.getScheduler().runTask(p, () -> showTo((Player) e.getPlayer()));
  }
}
