package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import dev.asoftglow.zvh.util.LimitedStack;
import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class Combat
{
  private static final Map<UUID, LimitedStack<UUID>> assist_history = new HashMap<>();
  private static final Map<UUID, Integer> human_kills = new HashMap<>();
  private static final Map<UUID, Integer> kills = new HashMap<>();

  @SuppressWarnings("unchecked")
  public static void handleKillRewards(Player player)
  {
    var killer = player.getKiller();
    if (killer != null && killer != player && Game.isPlaying(killer))
    {
      if (ZvH.humansTeam.hasPlayer(killer))
      {
        Database.changeIntStats(killer, Pair.of("coins", Rewards.COINS_ZOMBIE_KILL),
            Pair.of("xp", Rewards.XP_ZOMBIE_KILL), Pair.of("z_kills", 1));
        Rewards.S_changeCoins(killer, Rewards.COINS_ZOMBIE_KILL, "zombie kill");
        Database.S_changeXp(killer, Rewards.XP_ZOMBIE_KILL);

      } else if (ZvH.zombiesTeam.hasPlayer(killer))
      {
        Database.changeIntStats(killer, Pair.of("coins", Rewards.COINS_HUMAN_KILL),
            Pair.of("xp", Rewards.XP_HUMAN_KILL), Pair.of("h_kills", 1));
        Rewards.S_changeCoins(killer, Rewards.COINS_HUMAN_KILL, "human kill");
        Database.S_changeXp(killer, Rewards.XP_HUMAN_KILL);

        Util.playSoundAllAt(killer, Sound.ENTITY_ZOMBIE_INFECT, 0.9f, 1.2f);

        if (Game.getZombiesCount() == 1)
          return;

        // FIXME
        // Reviving

        final var ceiling = 3;
        var kills = human_kills.get(killer.getUniqueId());
        int left = ceiling - 1;
        if (kills == null)
        {
          human_kills.put(killer.getUniqueId(), Integer.valueOf(1));
        } else
        {
          if (kills.intValue() >= ceiling - 1)
          {
            Game.reviveZombie(killer);
            human_kills.remove(killer.getUniqueId());
            return;
          }
          human_kills.put(killer.getUniqueId(), Integer.valueOf(kills.intValue() + 1));
          left--;
        }
        killer.sendMessage(Component.text("Infect ").color(NamedTextColor.AQUA)
            .append(Component.text(left).color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)) //
            .append(Component
                .text(left == 1 ? " more human to become human again." : " more humans to become human again.")));
      }

      var r = kills.get(killer.getUniqueId());
      kills.put(killer.getUniqueId(), Integer.valueOf(r == null ? 1 : (r.intValue() + 1)));

      var wasProjectile = player.getLastDamageCause().getCause().equals(DamageCause.PROJECTILE);

      if (ZvH.zombiesTeam.hasPlayer(killer))
      {
        killer.getAdvancementProgress(ZvH.first_brains).awardCriteria("killh");

      } else if (ZvH.humansTeam.hasPlayer(killer))
      {
        killer.setHealth(Math.min(
            killer.getHealth() + (wasProjectile ? Rewards.HEALTH_ZOMBIE_KILL_PROJECTILE : Rewards.HEALTH_ZOMBIE_KILL),
            killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        killer.getAdvancementProgress(ZvH.first_blood).awardCriteria("killz");
      }

      // assists
      var assisters = assist_history.get(player.getUniqueId());
      if (assisters != null)
      {
        assisters.remove(killer.getUniqueId());
        assist_history.remove(player.getUniqueId());
        var players = Util.getPlayers(assisters);
        Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
          Rewards.changeCoinsAndXp(players, Rewards.COINS_HUMAN_ASSIST, Rewards.XP_HUMAN_ASSIST,
              "human assist");
        });
      }
    }
  }

  public static void handleDamage(Player player, Player damager)
  {
    if (ZvH.humansTeam.hasPlayer(player) && ZvH.zombiesTeam.hasPlayer(damager))
      addAssister(player, damager);
  }

  static void addAssister(Player target, Player assister)
  {
    var assisters = assist_history.get(target.getUniqueId());
    if (assisters == null)
    {
      assisters = new LimitedStack<>(4);
      assist_history.put(target.getUniqueId(), assisters);
    }
    assisters.add(assister.getUniqueId());
  }

  public static int getKills(Player player)
  {
    var r = kills.get(player.getUniqueId());
    return r == null ? 0 : r.intValue();
  }

  public static List<Map.Entry<UUID, Integer>> getKillers()
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
