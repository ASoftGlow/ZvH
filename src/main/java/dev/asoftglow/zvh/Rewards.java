package dev.asoftglow.zvh;

import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Handles visual indication of rewards and configuration. {@code S_} = shallow,
 * doesn't update database
 */
public abstract class Rewards
{
  public static final int COINS_JOIN = 10;
  public static final int COINS_ZOMBIE_KILL = 3;
  public static final int COINS_HUMAN_KILL = 10;
  public static final int COINS_HUMAN_ASSIST = 6;
  public static final int COINS_HUMAN_ALIVE = 1;
  public static final int COINS_HUMAN_WIN = 25;
  public static final int COINS_LAST_HUMAN_WIN = 35;
  public static final int COINS_ZOMBIE_WIN = 7;
  public static final int COINS_FIRST_ZOMBIE_WIN = 15;

  public static final int XP_ZOMBIE_KILL = 10;
  public static final int XP_HUMAN_KILL = 10;
  public static final int XP_HUMAN_ASSIST = 2;
  public static final int XP_HUMAN_WIN = 2;
  public static final int XP_LAST_HUMAN_WIN = 2;
  public static final int XP_ZOMBIE_WIN = 2;
  public static final int XP_FIRST_ZOMBIE_WIN = 2;

  public static final double HEALTH_ZOMBIE_KILL = 2d;
  public static final double HEALTH_ZOMBIE_KILL_PROJECTILE = 1d;

  public static int calcLvl(int xp)
  {
    return (int) Math.sqrt(xp + 196) - 14;
  }

  public static int calcXp(int lvl)
  {
    return (lvl + 14) * (lvl + 14) - 196;
  }

  public static void changeCoins(Player player, int amount, String reason)
  {
    if (amount == 0)
      return;
    S_changeCoins(player, amount, reason);
    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      Database.changeIntStat(player, "coins", amount);
    });
  }

  public static void changeCoins(Collection<Player> players, int amount, String reason)
  {
    if (amount == 0)
      return;
    S_changeCoins(players, amount, reason);

    Database.changeIntStat(players, "coins", amount);
  }

  public static void S_changeCoins(Player player, int amount, String reason)
  {
    player.sendActionBar(
        Component.text(reason == null ? "%+d coins".formatted(amount) : "%+d coins (%s)".formatted(amount, reason))
            .style(Style.style(NamedTextColor.GOLD)));
    SideBoard.updateCoins(player, amount);
  }

  public static void S_changeCoins(Collection<Player> players, int amount, String reason)
  {
    for (var p : players)
    {
      S_changeCoins(p, amount, reason);
    }
  }

  public static void displayExpBar(Player player, int lvl, int xp)
  {
    player.setExp((float) xp / Rewards.calcXp(lvl + 1));
    player.setLevel(lvl);
  }

  public static void handleLvlUp(Player player, int old, int lvl)
  {
    Util.playSoundAllAt(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.5f);
    if (lvl % 10 == 0)
    {
      Util.sendServerMsg(Component.empty().append(player.displayName().decorate(TextDecoration.BOLD))
          .append(Component.text(" has leveled up to "))
          .append(Component.text(lvl, NamedTextColor.GOLD, TextDecoration.BOLD)).append(Component.text('!')));
    }
  }
}