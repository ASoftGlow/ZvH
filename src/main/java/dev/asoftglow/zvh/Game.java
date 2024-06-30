package dev.asoftglow.zvh;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import dev.asoftglow.zvh.menus.ClassSelectionMenu;
import dev.asoftglow.zvh.menus.MapModifierMenu;
import dev.asoftglow.zvh.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class Game
{
  private static final int REQUIRED_PLAYERS = ZvH.isDev ? 1 : 2;
  private static final int SURVIVAL_TIME = ZvH.isDev ? 30 : 60 * 5;
  private static int current_survival_time;

  private static final Set<Player> last_zombies = new HashSet<>();
  public static final Set<Player> playing = new HashSet<>();
  public static final Set<Player> zombies = new HashSet<>();
  public static final Set<Player> humans = new HashSet<>();
  private static Game.State state = Game.State.STOPPED;
  private static int game_time;
  private static byte game_count = 0;
  private static Set<UUID> temp_blocked = new HashSet<>();

  public enum State
  {
    STOPPED, PLAYING, ENDING, LOADING;

    @Override
    public String toString()
    {
      return this.name().toLowerCase();
    }
  };

  public static Game.State getState()
  {
    return Game.state;
  }

  public static Component getStateText()
  {
    return switch (Game.getState())
    {
    case LOADING -> Component.text("Working...");
    case STOPPED -> Component.text("Waiting... %d/%d".formatted(ZvH.waitersTeam.getEntries().size(), REQUIRED_PLAYERS));
    case PLAYING, ENDING -> getFormatedTime();
    };
  }

  public static Component getFormatedTime()
  {
    return Component.text(String.format("Time: %d:%02d", game_time / 60, game_time % 60));
  }

  public static void clean()
  {
    for (var e : ZvH.humansTeam.getEntries())
    {
      var p = Bukkit.getOfflinePlayer(e);
      if (p.getUniqueId() == null)
        // non-player
        continue;
      ZvH.humansTeam.removePlayer(p);
    }

    for (var e : ZvH.zombiesTeam.getEntries())
    {
      var p = Bukkit.getOfflinePlayer(e);
      if (p.getUniqueId() == null)
        // non-player
        continue;
      ZvH.zombiesTeam.removePlayer(p);
    }

    for (var e : ZvH.waitersTeam.getEntries())
    {
      ZvH.waitersTeam.removeEntry(e);
    }
  }

  public static void joinWaiters(Player player)
  {
    ZvH.waitersTeam.addPlayer(player);
    if (ZvH.waitersTeam.getEntries().size() >= REQUIRED_PLAYERS)
    {
      if (ZvH.waitersTeam.getEntries().size() == REQUIRED_PLAYERS)
      {
        Utils.sendServerMsg("A game is starting soon!");
        startCountDown();
      }
      if (ZvH.waitersTeam.getEntries().size() == Bukkit.getOnlinePlayers().size())
      {
        if (count_down_time.intValue() > 4)
          count_down_time.setValue(4);
      }
    } else
    {
      SideBoard.updateGameState();
    }

    if (ZvH.waitersTeam.getEntries().size() == 1)
      waiting_task = Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
        for (var p : ZvH.waitersTeam.getEntries())
        {
          Bukkit.getPlayer(p).sendActionBar(Component.text("You are queued", Style.style(NamedTextColor.AQUA)));
        }
      }, 0, 60);
  }

  public static void leaveWaiters(Player player)
  {
    if (ZvH.waitersTeam.removePlayer(player))
    {
      if (ZvH.waitersTeam.getEntries().size() == REQUIRED_PLAYERS - 1)
      {
        cancelCountDown();
      } else
        SideBoard.updateGameState();
      if (ZvH.waitersTeam.getEntries().size() == 0)
      {
        waiting_task.cancel();
      }
    }
  }

  public static void joinSpectators(Player player)
  {
    if (MapControl.current_size == null)
      return;
    leaveWaiters(player);
    player.getInventory().clear();
    player.setItemOnCursor(null);
    player.getInventory().setItem(8, CustomItems.spec_leave);
    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 2, false, false, false));
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, -1, 255, false, false, false));
    player.teleport(new Location(player.getWorld(), MapControl.current_size.bounds().getCenter().getBlockX(),
        MapControl.current_size.bounds().y + MapControl.current_size.bounds().h + 1,
        MapControl.current_size.bounds().getCenter().getBlockZ(), 0f, 90f));
  }

  public static void leaveSpectators(Player player)
  {
    player.getInventory().clear();
    player.clearActivePotionEffects();
    player.setGameMode(GameMode.ADVENTURE);
    player.teleport(ZvH.worldSpawnLocation);
  }

  private static void join(Player player)
  {
    if (temp_blocked.contains(player.getUniqueId()))
    {
      player.sendMessage(Component
          .text("You left as the first and only zombie, which forces another zombie to be chosen. To prevent abuse, ")
          .color(NamedTextColor.RED)
          .append(Component.text("you must wait until next game to play again").decorate(TextDecoration.UNDERLINED))
          .append(Component.text(".")));
      Utils.playSound(player, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 0.8f);
      return;
    }
    playing.add(player);
    player.setGameMode(GameMode.SURVIVAL);
    player.setFallDistance(0);
    player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    player.getInventory().clear();
    player.setItemOnCursor(null);
    player.getInventory().setHeldItemSlot(0);
    player.clearActivePotionEffects();
    player.setRespawnLocation(
        MapControl.getLocation(player, MapControl.current_size.zombieSpawn(), MapControl.current_size.humanSpawn()),
        true);

    Music.stop(player);
  }

  public static void joinZombies(Player player)
  {
    ZvH.zombiesTeam.addPlayer(player);
    zombies.add(player);
    join(player);
    player.teleport(
        MapControl.getLocation(player, MapControl.current_size.zombieSpawn(), MapControl.current_size.humanSpawn()));
    ClassSelectionMenu.showTo(player);
    player.showTitle(Styles.zombie_spawn_title);
    Utils.playSound(player, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
  }

  public static void joinHumans(Player player, boolean revived)
  {
    ZvH.humansTeam.addPlayer(player);
    humans.add(player);
    join(player);

    var lvl = Database.getCachedIntStat(player, "lvl");
    SpeciesClassManager.getHumanClass(lvl.orElse(0)).giveTo(player);
    player.getInventory().setItem(8, CustomItems.shop_open);
    player.getInventory().addItem(new ItemStack(Material.ARROW, 3));

    if (revived)
      return;
    player.teleport(
        MapControl.getLocation(player, MapControl.current_size.humanSpawn(), MapControl.current_size.zombieSpawn()));
    player.showTitle(Styles.human_spawn_title);
    Utils.playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 1f, 0.8f);
  }

  public static void reviveZombie(Player player)
  {
    // FIXME
    player.getAdvancementProgress(ZvH.revival).awardCriteria("revival");
    Utils.playSoundAllAt(player, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.9f, 1.2f);
    Utils.sendServerMsg(Component.text(player.getName()).decorate(TextDecoration.BOLD)
        .append(Component.text(" has been become human again!").color(NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(Component.text("Infect 3 humans as a zombie"))));

    joinHumans(player, true);
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3, 1));
    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 3, 1));
  }

  public static void leaveHumans(Player player)
  {
    ZvH.humansTeam.removePlayer(player);
    humans.remove(player);
    if (humans.size() == 1)
    {
      var last_player = humans.iterator().next();
      last_player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1, 0));
    }
    joinZombies(player);
  }

  public static void leave(Player player)
  {
    leave(player, false);
  }

  public static void leave(Player player, boolean isQuiting)
  {
    player.clearTitle();
    player.clearActivePotionEffects();
    player.setFireTicks(0);
    player.setGameMode(GameMode.ADVENTURE);
    player.setArrowsInBody(0);
    player.setRespawnLocation(null, true);
    player.closeInventory();
    player.setFallDistance(0);
    player.setVelocity(new Vector(0, 0, 0));
    MiscListener.cancelPlayerLifeTasks(player);

    if (state == Game.State.PLAYING && playing.remove(player))
    {
      if (playing.size() == 0)
      {
        humans.remove(player);
        zombies.remove(player);
        stop();

      } else if (humans.remove(player))
      {
        if (humans.size() == 0)
        {
          rewardZombies();
          stop();
          announceZombiesWin();
        }
      } else if (zombies.remove(player))
      {
        ZvH.zombiesTeam.removePlayer(player);
        if (zombies.size() == 0)
        {
          // one human
          if (playing.size() == 1)
          {
            stop();
          } else
          {
            if (last_zombies.size() == 1 && last_zombies.iterator().next() == player)
            {
              temp_blocked.add(player.getUniqueId());
            }
            // choose new zombies
            for (var p : pickZombies(playing))
            {
              p.getInventory().clear();
              p.setItemOnCursor(null);
              leaveHumans(p);
              Utils.playSound(p, Sound.ENTITY_ZOMBIE_AMBIENT, 50f, 0.8f);
            }
          }
        }
        if (isQuiting)
        {
          last_zombies.remove(player);
        }
      }
    } else
    {
      leaveWaiters(player);
    }
    ZvH.zombiesTeam.removePlayer(player);
    ZvH.humansTeam.removePlayer(player);

    player.closeInventory();
    player.getInventory().clear();
    player.setItemOnCursor(null);
    player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    player.teleport(ZvH.worldSpawnLocation);

    if (!isQuiting)
    {
      Music.playLobby(player);
    }
  }

  public static boolean isPlaying(Player player)
  {
    return playing.contains(player);
  }

  public static void start()
  {
    state = Game.State.LOADING;
    for (var e : ZvH.waitersTeam.getEntries())
    {
      Player player = Bukkit.getPlayer(e);
      if (player.isConnected())
      {
        playing.add(player);
      }
    }
    if (game_count++ % 2 == 0)
    {
      MapModifierMenu.showTo(playing, Game::_start);

    } else
    {
      MapControl.chooseMap(ZvH.waitersTeam.getEntries().size());
      _start();
    }
  }

  private static void _start()
  {
    MapControl.resetMap();

    ClassSelectionMenu.reset();
    temp_blocked.clear();
    state = Game.State.PLAYING;
    game_time = current_survival_time = SURVIVAL_TIME;// + Math.min(10, playing.size()) * 15;
    for (var p : playing)
    {
      if (p.isConnected())
      {
        p.sendActionBar(Component.empty());
        ZvH.waitersTeam.removePlayer(p);
        SideBoard.updateGameState(p);
        p.closeInventory();
      }
    }

    for (var player : pickZombies(playing))
    {
      joinZombies(player);
    }

    for (var player : playing)
    {
      if (!zombies.contains(player))
      {
        joinHumans(player, false);
      }
    }

    // blocks
    game_tasks.add(Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      for (var player : playing)
      {
        if (ClassSelectionMenu.hasMenuOpen(player))
          continue;
        player.getInventory().addItem(new ItemStack(Cosmetics.Blocks.getFallMaterialFor(player), 4),
            new ItemStack(Cosmetics.Blocks.getSolidMaterialFor(player), 2));
      }
    }, 0, 20 * 10));

    // time
    game_tasks.add(Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      updateTime();
    }, 0, 20));

    game_tasks.add(Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      for (var p : playing)
      {
        if (!MapControl.current_size.bounds().contains(p.getLocation().getBlock()))
        {
          p.setHealth(0);
        }
      }
    }, 0, 20 * 30));

    Utils.sendServerMsg("A game has started.");
  }

  public static void stopGameTasks()
  {
    while (!game_tasks.empty())
      game_tasks.pop().cancel();
  }

  public static void stop()
  {
    state = Game.State.STOPPED;
    Bukkit.getServer().getServerTickManager().setTickRate(20);
    stopGameTasks();

    final var style = Style.style(NamedTextColor.WHITE, TextDecoration.BOLD);
    final var killers = Combat.getKillers();
    Component killers_msg;
    if (killers.size() == 0)
    {
      killers_msg = Component.text("\n\n  There were no killers.");

    } else
    {
      killers_msg = Component.text("\n\n  Top killers:");
      for (int i = 0; i < Math.min(killers.size(), 5); i++)
      {
        var player = Bukkit.getPlayer(killers.get(i).getKey());
        killers_msg = killers_msg
            .append(Component.text("\n    %-2d ".formatted(killers.get(i).getValue().intValue()), style)
                .append(player == null ? Component.text("?") : player.displayName()));
      }
    }

    for (var p : playing)
    {
      p.sendMessage(Component.newline()
          .append(Component.text("Game Summary\n\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED))
          .append(Component.text("  Your kills: ")).append(Component.text(Combat.getKills(p), style))
          .append(killers_msg).appendNewline() //
      );

      leave(p);
    }
    zombies.clear();
    humans.clear();
    playing.clear();
    Combat.reset();
    ClassSelectionMenu.reset();

    ZvH.updateLeaderboards();
    SideBoard.updateGameState();
    Utils.sendServerMsg("This game has ended.");
  }

  private static BukkitTask count_down_task, waiting_task;
  private static Stack<BukkitTask> game_tasks = new Stack<>();
  private static final MutableInt count_down_time = new MutableInt(0);

  public static void startCountDown()
  {
    count_down_time.setValue(15 + 1);
    count_down_task = Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      count_down_time.subtract(1);
      if (count_down_time.intValue() == 0)
      {
        cancelCountDown();
        waiting_task.cancel();
        start();
        return;
      }
      SideBoard.updateCountDown(count_down_time.intValue());
    }, 0, 20);
  }

  public static void cancelCountDown()
  {
    count_down_task.cancel();
    SideBoard.updateGameState();
  }

  private static void updateTime()
  {
    if (--game_time < 0)
    {
      game_time = 0;
      rewardHumans();
      stop();
      announceHumansWin();
      return;
    }
    SideBoard.updateGameState();
    if (game_time != 0 && game_time % 60 == 0)
    {
      Rewards.changeCoins(humans, Rewards.COINS_HUMAN_ALIVE, "staying alive");
    }
  }

  public static Set<Player> pickZombies(Set<Player> players)
  {
    int num_chosen = players.size() / 6 + 1;
    var options = new HashSet<>(players);

    var it = last_zombies.iterator();
    while (it.hasNext() && options.size() > num_chosen)
    {
      options.remove(it.next());
    }
    last_zombies.clear();

    for (int i = 0; i < num_chosen; i++)
    {
      last_zombies.add(Utils.popRandom(options));
    }
    return last_zombies;
  }

  public static void play(Player player)
  {
    if (isPlaying(player))
    {
      player.sendMessage("You're already playing, silly!");
      return;
    }
    switch (state)
    {
    case LOADING:
    case ENDING:
      player.sendMessage(Component.text("Please wait a moment...").color(NamedTextColor.RED));
      break;

    case STOPPED:
      if (ZvH.waitersTeam.hasPlayer(player))
      {
        leaveWaiters(player);
        player.sendMessage(Component.text("Left queue").color(NamedTextColor.RED));
      } else
      {
        joinWaiters(player);
        Utils.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
        player.sendMessage(Component.text("Joined queue").color(NamedTextColor.GREEN));
      }
      break;

    case PLAYING:
      joinZombies(player);
      break;
    }
  }

  private static int getPlaytime()
  {
    return (current_survival_time - game_time) / 60;
  }

  public static void announceZombiesWin()
  {
    for (var p : Bukkit.getOnlinePlayers())
    {
      p.showTitle(Styles.zombies_win_title);
      Utils.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1f);
    }
  }

  @SuppressWarnings("unchecked")
  public static void rewardZombies()
  {
    int playtime = getPlaytime();
    var infected_zombies = new HashSet<>(zombies);

    if (last_zombies.size() > 0)
    {
      infected_zombies.removeAll(last_zombies);

      // first zombies
      Database.S_changeXp(last_zombies, Rewards.XP_FIRST_ZOMBIE_WIN);
      Rewards.S_changeCoins(last_zombies, Rewards.COINS_FIRST_ZOMBIE_WIN,
          last_zombies.size() == 1 ? "won as first zombie" : "won as a first zombie");
    }

    // zombies
    Database.S_changeXp(last_zombies, Rewards.XP_ZOMBIE_WIN);
    Rewards.S_changeCoins(last_zombies, Rewards.COINS_ZOMBIE_WIN, "won");

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      if (last_zombies.size() > 0)
      {
        // first zombies
        Database.changeIntStats(last_zombies, Pair.of("z_wins", 1), Pair.of("play_time", playtime),
            Pair.of("coins", Rewards.COINS_FIRST_ZOMBIE_WIN), Pair.of("xp", Rewards.XP_FIRST_ZOMBIE_WIN));
      }

      Database.changeIntStats(infected_zombies, Pair.of("z_wins", 1), Pair.of("play_time", playtime),
          Pair.of("coins", Rewards.COINS_ZOMBIE_WIN), Pair.of("xp", Rewards.XP_ZOMBIE_WIN));

      // humans
      Database.changeIntStats(humans, Pair.of("h_losses", 1), Pair.of("play_time", playtime));
    });
  }

  public static void announceHumansWin()
  {
    for (var p : Bukkit.getOnlinePlayers())
    {
      p.showTitle(Styles.humans_win_title);
      Utils.playSound(p, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
    }
  }

  @SuppressWarnings("unchecked")
  public static void rewardHumans()
  {
    int playtime = getPlaytime();
    var last_human = humans.size() == 1 && playing.size() > 2 ? humans.iterator().next() : null;

    if (last_human != null)
    {
      Database.S_changeXp(last_human, Rewards.XP_LAST_HUMAN_WIN);
      Rewards.S_changeCoins(last_human, Rewards.COINS_LAST_HUMAN_WIN, "won as last human");

    } else
    {
      Database.S_changeXp(humans, Rewards.XP_LAST_HUMAN_WIN);
      Rewards.S_changeCoins(humans, Rewards.COINS_HUMAN_WIN, "won");
    }

    Bukkit.getScheduler().runTaskAsynchronously(ZvH.singleton, () -> {
      if (last_human != null)
      {
        // last human
        Database.changeIntStats(last_human, Pair.of("h_wins", 1), Pair.of("play_time", playtime),
            Pair.of("coins", Rewards.COINS_LAST_HUMAN_WIN), Pair.of("xp", Rewards.XP_LAST_HUMAN_WIN));
      } else
      {
        // humans
        Database.changeIntStats(humans, Pair.of("h_wins", 1), Pair.of("play_time", playtime),
            Pair.of("coins", Rewards.COINS_HUMAN_WIN), Pair.of("xp", Rewards.XP_HUMAN_WIN));
      }

      // zombies
      Database.changeIntStats(zombies, Pair.of("z_losses", 1), Pair.of("play_time", playtime));
    });
  }

  public static void handleDeath(PlayerDeathEvent e)
  {
    if (state == Game.State.ENDING)
    {
      e.setCancelled(true);
      return;
    }
    var player = e.getPlayer();
    // kill rewards
    Combat.handleKillRewards(player);

    // last human
    if (humans.contains(player) && humans.size() == 1)
    {
      state = Game.State.ENDING;
      final var tm = player.getServer().getServerTickManager();
      // keep player alive
      e.setCancelled(true);
      tm.setTickRate(5);
      stopGameTasks();

      ClassSelectionMenu.reset();
      for (var p : playing)
      {
        p.setGameMode(GameMode.ADVENTURE);
        p.closeInventory();
        p.playSound(player, e.getDeathSound(), 1f, 1f);
        p.sendMessage(e.deathMessage());
      }
      announceZombiesWin();
      rewardZombies();

      Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
        tm.setTickRate(20);
        for (var p : playing)
        {
          p.setGameMode(GameMode.SPECTATOR);
        }
        Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
          stop();
        }, 20 * 5);
      }, 20 * 2);
    }
    player.getInventory().clear();
    player.setItemOnCursor(null);
    e.setDroppedExp(0);
    e.setNewTotalExp(0);

    MiscListener.cancelPlayerLifeTasks(player);
    DiscordBot.sendMessage(e.deathMessage());
  }
}
