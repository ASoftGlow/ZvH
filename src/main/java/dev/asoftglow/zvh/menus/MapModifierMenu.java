package dev.asoftglow.zvh.menus;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.lang.Runnable;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.MapControl;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.MapControl.MapFeature;
import dev.asoftglow.zvh.util.Utils;
import dev.asoftglow.zvh.util.guilib.VotingMenu;
import xyz.janboerman.guilib.api.ItemBuilder;

public abstract class MapModifierMenu
{
  public interface MapModifier
  {
    public String getName();

    public String getDescription();

    public String getType();

    public Material getIcon();
  }

  private static VotingMenu<ZvH> menu;
  private static List<MapFeature> feats;

  // private final static SelectButton<ZvH> closeBtn = new SelectButton<ZvH>(
  // (new
  // ItemBuilder(Material.BARRIER)).flags(ItemFlag.HIDE_ENCHANTS).name("§r§5Leave").build(),
  // e -> {
  // shouldLeave.add((Player) e.getWhoClicked());
  // Game.leave((Player) e.getWhoClicked());
  // return true;
  // });

  static
  {
    menu = new VotingMenu<>(ZvH.singleton, 9, "Vote for a Modifier", winner_i -> {
      if (winner_i == null)
      {
        Utils.sendServerMsg("Nothing won.");
        MapControl.chooseMap(menu.getPlayers().size());

      } else
      {
        var winner = feats.get(winner_i.intValue());
        Utils.sendServerMsg(winner.name + " had the most votes.");

        MapControl.chooseMapSize(menu.getPlayers().size());
        MapControl.current_feature = winner;
      }
      menu.reset();

    }, 10);
  }

  public static void showTo(Collection<Player> players, Runnable callback)
  {
    final MapModifier nothing = MapControl.features[MapControl.features.length - 1];
    feats = MapControl.getFeatures(players.size());
    feats.removeIf(f -> f == nothing);
    Collections.shuffle(feats);

    int i;
    for (i = 0; i < feats.size() && i < 3; i++)
    {
      var item = new ItemBuilder(feats.get(i).getIcon()).name("§r§f" + feats.get(i).getName())
          .lore("§r§8" + feats.get(i).getDescription(), feats.get(i).getType()).build();

      menu.setButton(i, item);
    }
    menu.setButton(i++, new ItemBuilder(nothing.getIcon()).name("§r§f" + nothing.getName())
        .lore("§r§8" + nothing.getDescription(), nothing.getType()).build());
    while (i < 4)
    {
      menu.setButton(i++, null);
    }
    menu.callback2 = callback;
    menu.showTo(players);
  }

  public static void showTo(Player player)
  {
    var list = new ArrayList<Player>();
    list.add(player);
    showTo(list, () -> {});
  }
}
