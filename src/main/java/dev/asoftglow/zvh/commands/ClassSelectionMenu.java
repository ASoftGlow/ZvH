package dev.asoftglow.zvh.commands;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.janboerman.guilib.api.ItemBuilder;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import dev.asoftglow.zvh.CustomItems;
import dev.asoftglow.zvh.Database;
import dev.asoftglow.zvh.Rewards;
import dev.asoftglow.zvh.ZombieClass;
import dev.asoftglow.zvh.SpeciesClassManager;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.Util;
import dev.asoftglow.zvh.util.guilib.SelectButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClassSelectionMenu implements Listener
{
  private static MenuHolder<ZvH> menu;
  private static Set<Player> shouldLeave = new HashSet<>();

  private boolean SelectionHandler(Player player, ZombieClass zClass)
  {
    Database.getIntStat(player, "coins", coins -> {

      player.clearActivePotionEffects();
      Util.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
      if (player.getGameMode() == GameMode.CREATIVE)
      {
        zClass.giveTo(player);
        player.getInventory().setItem(8, CustomItems.shop_open);
        shouldLeave.remove(player);
        player.closeInventory();

      } else if (coins.isPresent() && coins.getAsInt() >= zClass.price)
      {
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 5));
        Rewards.changeCoins(player, -zClass.price, "shopping");
        zClass.giveTo(player);
        player.getInventory().setItem(8, CustomItems.shop_open);
        shouldLeave.remove(player);
        player.closeInventory();
      }
    });
    return false;
  }

  public ClassSelectionMenu(ZvH zvh)
  {
    menu = new MenuHolder<>(zvh, 9, "Choose a class:");

    menu.setButton(8, new SelectButton<ZvH>((new ItemBuilder(Material.BARRIER)).name("§r§6Leave").build(), e -> {
      e.getWhoClicked().clearActivePotionEffects();
      Util.playSound((Player) e.getWhoClicked(), Sound.UI_BUTTON_CLICK, 1, 1);
      return true;
    }));

    int i = 0;
    for (var c : SpeciesClassManager.speciesGetClassNames("zombie"))
    {
      var zClass = (ZombieClass) SpeciesClassManager.speciesGetClass("zombie", c);
      var item = new ItemBuilder(zClass.icon).name("§r§f" + zClass.name.replace('_', ' '));
      if (zClass.price > 0)
        item = item.lore("Costs " + zClass.price);
      menu.setButton(i++,
          new SelectButton<ZvH>(item.build(), e -> SelectionHandler((Player) e.getWhoClicked(), zClass)));
    }
  }

  public static void showTo(Player player)
  {
    shouldLeave.add(player);
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, -1, 255, false, false, false));
    player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, -1, 255, false, false, false));
    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 255, false, false, false));
    player.openInventory(menu.getInventory());
  }

  public static boolean hasMenuOpen(Player player)
  {
    return shouldLeave.contains(player);
  }

  public static void reset()
  {
    shouldLeave.clear();
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e)
  {
    if (e.getPlayer() instanceof Player && e.getView().getOriginalTitle().equals("Choose a class:"))
    {
      var player = (Player) e.getPlayer();
      if (shouldLeave.remove(player))
      {
        player.sendMessage(Component.text("You closed the menu with picked a class, so you were given the default one.")
            .color(NamedTextColor.RED));
        player.clearActivePotionEffects();
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 5));
        // give first
        SpeciesClassManager.speciesGetClass("zombie", "Zombie").giveTo(player);
        player.getInventory().setItem(8, CustomItems.shop_open);
      }
    }
  }
}
