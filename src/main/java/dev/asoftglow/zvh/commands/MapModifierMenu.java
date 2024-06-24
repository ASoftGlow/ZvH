package dev.asoftglow.zvh.commands;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.lang.Runnable;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.MapControl;
import dev.asoftglow.zvh.ZvH;
import dev.asoftglow.zvh.MapControl.MapFeature;
import dev.asoftglow.zvh.util.Util;
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
        Util.sendServerMsg("Nothing won.");
        MapControl.chooseMap(menu.getPlayers().size());

      } else
      {
        var winner = feats.get(winner_i.intValue());
        Util.sendServerMsg(winner.name + " had the most votes.");

        MapControl.chooseMapSize(menu.getPlayers().size());
        MapControl.current_feature = winner;
      }
      menu.reset();

    }, 10);
  }

  public static void showTo(Collection<Player> players, Runnable callback)
  {
    feats = MapControl.getFeatures(players.size());
    int i = 0;
    for (MapModifier modifier : feats)
    {
      if (modifier.getName() == null)
        continue;

      var item = new ItemBuilder(modifier.getIcon()).name("§r§f" + modifier.getName())
          .lore("§r§8" + modifier.getDescription(), modifier.getType()).build();

      menu.setButton(i++, item);
    }
    while (i < 9)
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
    menu.showTo(list);
  }
}
