package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.util.Utils;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public abstract class SideBoard
{
  private static final TextComponent board_title = Component.text().append(Component.text("Z", Styles.zombie_style))
      .append(Component.text("v")).append(Component.text("H", Styles.human_style)).build();

  private static final HashMap<Player, FastBoard> boards = new HashMap<>();

  public static void update(FastBoard fb)
  {
    var coins = Database.getCachedIntStat(fb.getPlayer(), "coins");
    var lines = new ArrayList<Component>();

    lines.add(Component.empty());
    lines.add(Game.getStateText());
    lines.add(Component.empty());
    lines.add(Component.text("Coins: " + (coins.isPresent() ? coins.getAsInt() : "?")));
    lines.add(Component.empty());

    fb.updateLines(lines);
  }

  public static void updateCoins(Player player, int coins_change)
  {
    var coins = Database.getCachedIntStat(player, "coins");
    getBoard(player).updateLine(3,
        Component.text("Coins: " + (coins.isPresent() ? coins.getAsInt() + coins_change : "?")));
  }

  private static FastBoard getBoard(Player player)
  {
    var fb = boards.get(player);
    if (fb == null)
    {
      fb = addBoard(player);
    }
    return fb;
  }

  public static FastBoard addBoard(Player player)
  {
    var fb = new FastBoard(player);
    fb.updateTitle(board_title);
    boards.put(player, fb);
    update(fb);
    return fb;
  }

  public static void removeBoard(Player player)
  {
    var fb = boards.remove(player);
    if (fb != null)
    {
      fb.delete();
    }
  }

  public static void updateCountDown(int t)
  {
    for (var fb : boards.values())
    {
      fb.updateLine(1, Component.text("Starting in " + t));
      Utils.playSound(fb.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 2f);
    }
  }

  public static void updateGameState()
  {
    for (final var fb : boards.values())
    {
      updateGameState(fb);
    }
  }

  public static void updateGameState(Player player)
  {
    updateGameState(getBoard(player));
  }

  public static void updateGameState(FastBoard fb)
  {
    fb.updateLine(1, Game.getStateText());
  }
}
