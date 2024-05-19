package dev.asoftglow.zvh;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public abstract class Combat
{
  private static final Map<Player, Set<Player>> assistHistory = new HashMap<>();

  public static void handleKillRewards(Player player)
  {
    var killer = player.getKiller();
    if (killer != null && killer != player && Game.isPlaying(killer))
    {
      ZvH.changeExp(killer, 10);

      if (ZvH.humansTeam.hasPlayer(killer))
        ZvH.changeCoins(killer, Rewards.COIN_ZOMBIE_KILL, "zombie kill");
      else
        ZvH.changeCoins(killer, Rewards.COIN_HUMAN_KILL, "human kill");

      var wasProjectile = player.getLastDamageCause().getCause().equals(DamageCause.PROJECTILE);

      if (ZvH.zombiesTeam.hasPlayer(player))
      {
        killer.setHealth(Math.min(
            killer.getHealth() + (wasProjectile ? Rewards.HEALTH_ZOMBIE_KILL_PROJECTILE : Rewards.HEALTH_ZOMBIE_KILL),
            20d));
        killer.getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_brains")))
            .awardCriteria("killh");
      } else if (ZvH.humansTeam.hasPlayer(player))
      {
        killer.getAdvancementProgress(Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_blood")))
            .awardCriteria("killz");
      }
    }

    // assists
    var assisters = assistHistory.get(player);
    if (assisters != null)
    {
      for (var a : assisters)
      {
        if (a == killer)
          continue;
        ZvH.changeExp(a, 2);
        ZvH.changeCoins(a, Rewards.COIN_HUMAN_ASSIST, "human assist");
      }
      assistHistory.remove(player);
    }
  }

  public static void handleDamage(Player player, Player damager)
  {
    if (ZvH.humansTeam.hasPlayer(player) && ZvH.zombiesTeam.hasPlayer(damager))
      addAssister(player, damager);
  }

  static void addAssister(Player target, Player assister)
  {
    var assisters = assistHistory.get(target);
    if (assisters == null)
    {
      assisters = new HashSet<Player>();
      assistHistory.put(target, assisters);
    }
    assisters.add(assister);
  }

  public static void reset()
  {
    assistHistory.clear();
  }
}
