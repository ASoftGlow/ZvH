package dev.asoftglow.zvh.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.*;
import dev.asoftglow.zvh.Game;
import dev.asoftglow.zvh.ZClass;
import dev.asoftglow.zvh.ZClassManager;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.SelectButton;

public class ClassSelectionMenu implements Listener
{
  private static MenuHolder<ZvH> menu;
  private static InventoryView lastView;
  private static Set<Player> opens = new HashSet<>();

  private boolean SelectionHandler(Player player, ZClass zClass)
  {
    Game.zombie_class.put(player, zClass);
    player.clearActivePotionEffects();
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 5));
    if (player.getGameMode() == GameMode.CREATIVE)
    {
      player.addScoreboardTag("clicked");
      zClass.give(player);
      return true;
    }
    if (ZvH.coins.getScore(player).getScore() >= zClass.price)
    {
      ZvH.changeCoins(player, -zClass.price, "shopping");
      player.addScoreboardTag("clicked");
      zClass.give(player);

      if (player.getName().equals("AthenaViolet"))
      {
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
      }
      return true;
    }
    return false;
  }

  public ClassSelectionMenu(ZvH zvh)
  {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new SelectButton<ZvH>((new ItemBuilder(Material.BARRIER)).name("§r§6Leave").build(), e -> {
      e.getWhoClicked().addScoreboardTag("clicked");
      Game.leave((Player) e.getWhoClicked());
      return true;
    }));

    int i = 0;
    for (var zClass : ZClassManager.zClasses.values())
    {
      var item = new ItemBuilder(zClass.icon).name("§r§f" + zClass.name.replace('_', ' '));
      if (zClass.price > 0)
        item = item.lore("Costs " + zClass.price);
      menu.setButton(i++,
          new SelectButton<ZvH>(item.build(), e -> SelectionHandler((Player) e.getWhoClicked(), zClass)));
    }
  }

  public static void showTo(Player player)
  {
    opens.add(player);
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, -1, 255, false, false, false));
    lastView = player.openInventory(menu.getInventory());
  }

  public static boolean hasMenuOpen(Player player)
  {
    return opens.contains(player);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e)
  {
    if (e.getPlayer() instanceof Player && lastView == e.getView())
    {
      opens.remove(e.getPlayer());
      if (e.getPlayer().getScoreboardTags().contains("clicked"))
        e.getPlayer().removeScoreboardTag("clicked");
      else
        Bukkit.getScheduler().runTask(ZvH.singleton, () -> showTo((Player) e.getPlayer()));
    }
  }
}
