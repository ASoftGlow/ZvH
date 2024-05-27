package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class Combat
{
  private static final Map<Player, Set<Player>> assist_history = new HashMap<>();
  private static final Map<Player, Integer> human_kills = new HashMap<>();
  private static final Map<Player, Integer> kills = new HashMap<>();

  public static void handleKillRewards(Player player)
  {
    var killer = player.getKiller();
    if (killer != null && killer != player && Game.isPlaying(killer))
    {
      ZvH.changeExp(killer, 10);

      if (ZvH.humansTeam.hasPlayer(killer))
      {
        ZvH.changeCoins(killer, Rewards.COIN_ZOMBIE_KILL, "zombie kill");

      } else if (ZvH.zombiesTeam.hasPlayer(killer))
      {
        ZvH.changeCoins(killer, Rewards.COIN_HUMAN_KILL, "human kill");
        Util.playSoundAllAt(killer, Sound.ENTITY_ZOMBIE_INFECT, 0.9f, 1.2f);

        // FIXME
        // if (Game.getZombiesCount() == 1)
        // return;

        // final var ceiling = 1;
        // var kills = human_kills.get(killer);
        // int left = ceiling - 1;
        // if (kills == null)
        // {
        // human_kills.put(killer, Integer.valueOf(1));
        // } else
        // {
        // if (kills.intValue() >= ceiling - 1)
        // {
        // Game.reviveHuman(killer);
        // human_kills.remove(killer);
        // return;
        // }
        // human_kills.put(killer, Integer.valueOf(kills.intValue() + 1));
        // left--;
        // }
        // killer.sendMessage(Component.text("Infect ").color(NamedTextColor.AQUA)
        // .append(Component.text(left).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)).append(Component
        // .text(left == 1 ? " more human to become human again." : " more humans to
        // become human again.")));
      }

      var r = kills.get(killer);
      kills.put(killer, Integer.valueOf(r == null ? 1 : (r.intValue() + 1)));

      var wasProjectile = player.getLastDamageCause().getCause().equals(DamageCause.PROJECTILE);

      if (ZvH.zombiesTeam.hasPlayer(killer))
      {
        killer.getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_brains")))
            .awardCriteria("killh");
      } else if (ZvH.humansTeam.hasPlayer(killer))
      {
        killer.setHealth(Math.min(
            killer.getHealth() + (wasProjectile ? Rewards.HEALTH_ZOMBIE_KILL_PROJECTILE : Rewards.HEALTH_ZOMBIE_KILL),
            20d));
        killer.getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_blood")))
            .awardCriteria("killz");
      }
    }

    // assists
    var assisters = assist_history.get(player);
    if (assisters != null)
    {
      for (var a : assisters)
      {
        if (a == killer)
          continue;
        ZvH.changeExp(a, 2);
        ZvH.changeCoins(a, Rewards.COIN_HUMAN_ASSIST, "human assist");
      }
      assist_history.remove(player);
    }
  }

  public static void handleDamage(Player player, Player damager)
  {
    if (ZvH.humansTeam.hasPlayer(player) && ZvH.zombiesTeam.hasPlayer(damager))
      addAssister(player, damager);
  }

  static void addAssister(Player target, Player assister)
  {
    var assisters = assist_history.get(target);
    if (assisters == null)
    {
      assisters = new HashSet<Player>();
      assist_history.put(target, assisters);
    }
    assisters.add(assister);
  }

  public static int getKills(Player player)
  {
    var r = kills.get(player);
    return r == null ? 0 : r.intValue();
  }

  public static List<Map.Entry<Player, Integer>> getKillers()
  {
    var entries = new ArrayList<>(kills.entrySet());
    entries.sort((a, b) -> {
      return b.getValue() - a.getValue();
    });

    return entries;
  }

  public static void reset()
  {
    assist_history.clear();
    kills.clear();
  }
}
