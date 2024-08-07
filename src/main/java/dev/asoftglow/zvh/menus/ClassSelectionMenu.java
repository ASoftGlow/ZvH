package dev.asoftglow.zvh.menus;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;

import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.janboerman.guilib.api.menu.ItemButton;
import xyz.janboerman.guilib.api.menu.MenuHolder;
import dev.asoftglow.zvh.CustomItems;
import dev.asoftglow.zvh.Database;
import dev.asoftglow.zvh.Rewards;
import dev.asoftglow.zvh.ZombieClass;
import dev.asoftglow.zvh.SpeciesClassManager;
import dev.asoftglow.zvh.Styles;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.util.Utils;
import dev.asoftglow.zvh.util.guilib.CloseButton;
import dev.asoftglow.zvh.util.guilib.PredicateButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ClassSelectionMenu extends MenuHolder<ZvH>
{
  private static final Set<PotionEffect> idle_buffs = Set.of(
      new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, -1, 255, false, false, false),
      new PotionEffect(PotionEffectType.WEAKNESS, -1, 255, false, false, false),
      new PotionEffect(PotionEffectType.FIRE_RESISTANCE, -1, 255, false, false, false));
  private static Set<Player> shouldLeave = new HashSet<>();

  private List<String> classes = null;
  private int coins, lvl;

  private static final BiPredicate<ClassSelectionMenu, InventoryClickEvent> bp = (h, e) -> {
    var player = (Player) e.getWhoClicked();
    var c = h.classes.get(e.getSlot());
    var zClass = (ZombieClass) SpeciesClassManager.speciesGetClass("zombie", c);
    if (h.coins >= zClass.price && h.lvl >= zClass.min_lvl)
    {
      if (player.getGameMode() == GameMode.SURVIVAL)
      {
        Rewards.changeCoins(player, -zClass.price, "shopping");
      }
      player.clearActivePotionEffects();
      player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2, 4));
      zClass.giveTo(player);
      player.getInventory().setItem(8, CustomItems.shop_open);
      shouldLeave.remove(player);
      Utils.playSound(player, Sound.UI_BUTTON_CLICK, 0.9f, 1f);
      return false;
    }

    Utils.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.9f, 1f);
    return true;
  };

  private ClassSelectionMenu(Player player)
  {
    super(ZvH.singleton, 9, "Choose a zombie class:");

    coins = Database.getCachedIntStat(player, "coins").orElse(0);
    lvl = Database.getCachedIntStat(player, "lvl").orElse(0);

    classes = SpeciesClassManager.speciesGetClassNames("zombie");
    for (int i = 0; i < classes.size(); i++)
    {
      var zClass = (ZombieClass) SpeciesClassManager.speciesGetClass("zombie", classes.get(i));
      var item = new ItemStack(zClass.icon);
      var meta = item.getItemMeta();
      final boolean isAffordable = zClass.price <= coins;
      final boolean isLeveled = zClass.min_lvl <= lvl;

      meta.displayName(Component.text(zClass.name.replace('_', ' '),
          isAffordable && isLeveled ? Styles.affordable_style : Styles.too_expensive_style));
      Utils.addLore(meta,
          Component.text("Costs ", NamedTextColor.GRAY)
              .append(Component.text(zClass.price, isAffordable ? NamedTextColor.GOLD : NamedTextColor.RED))
              .decoration(TextDecoration.ITALIC, false));
      if (zClass.min_lvl > 0)
      {
        Utils.addLore(meta,
            Component.text("Level ", NamedTextColor.GRAY)
                .append(Component.text(zClass.min_lvl, isLeveled ? NamedTextColor.AQUA : NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
      }
      item.setItemMeta(meta);

      setButton(i, new PredicateButton<>(new ItemButton<>(item), bp));
    }
    setButton(8, new CloseButton<>());

    player.openInventory(getInventory());
  }

  public static void showTo(Player player)
  {
    shouldLeave.add(player);
    player.addPotionEffects(idle_buffs);

    new ClassSelectionMenu(player);
  }

  public static boolean hasMenuOpen(Player player)
  {
    return shouldLeave.contains(player);
  }

  public static void reset()
  {
    shouldLeave.clear();
  }

  @Override
  public void onClose(InventoryCloseEvent e)
  {
    var player = (Player) e.getPlayer();
    if (shouldLeave.remove(player))
    {
      player.clearActivePotionEffects();
      player
          .sendMessage(Component.text("You closed the menu without picking a class, so you were given the default one.")
              .color(NamedTextColor.RED));
      player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 2, 2));
      // give first
      SpeciesClassManager.speciesGetClass("zombie", "Zombie").giveTo(player);
      player.getInventory().setItem(8, CustomItems.shop_open);
    }
  }
}
