package dev.asoftglow.zvh.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.*;
import dev.asoftglow.zvh.GetZClass;
import dev.asoftglow.zvh.SelectButton;
import dev.asoftglow.zvh.ZClass;
import dev.asoftglow.zvh.ZvH;

public class ClassCommand implements CommandExecutor {
  private final MenuHolder<ZvH> menu;

  private void SelectionHandler(Player player, ZClass zClass) {
    GetZClass.giveKit(player);
  }

  public ClassCommand(ZvH zvh) {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new CloseButton<>(Material.BARRIER, "§rClose"));
    for (int i = 0; i < zvh.zClasses.length; i++) {
      var zclass = zvh.zClasses[i];
      var item = new ItemBuilder(zclass.icon()).name("§r" + zclass.name()).lore("Costs " + zclass.price()).build();
      menu.setButton(i, new SelectButton<ZvH>(item, e -> SelectionHandler((Player) e.getWhoClicked(), zclass)));
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label,
      String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      player.openInventory(menu.getInventory());
    }
    return true;
  }
}
