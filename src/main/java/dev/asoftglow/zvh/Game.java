package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;

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

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.util.Util;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

public abstract class Game
{
  private static final int REQUIRED_PLAYERS = ZvH.isDev ? 1 : 2;
  private static final int SURIVAL_TIME = 60 * 5;
  private static HashMap<Player, FastBoard> boards = new HashMap<>();
  private static final Style zombie_style = Style.style(NamedTextColor.DARK_GREEN, TextDecoration.BOLD);
  private static final Style human_style = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
  private static final TextComponent board_title = Component.text().append(Component.text("Z", zombie_style))
      .append(Component.text("v")).append(Component.text("H", human_style)).build();
  private static final Title zombie_spawn_title = Title.title(Component.text("Zombie", zombie_style),
      Component.text("Kill the humans!"));
  private static final Title human_spawn_title = Title.title(Component.text("Human", human_style),
      Component.text("Don't get killed!"));
  private static final Title zombies_win_title = Title.title(Component.text("Zombies", zombie_style),
      Component.text("have won!"));
  private static final Title humans_win_title = Title.title(Component.text("Humans", human_style),
      Component.text("have won!"));

  private static Set<Player> last_zombies = new HashSet<>();
  public static Set<Player> playing = new HashSet<>();
  private static Game.State state = Game.State.STOPPED;
  private static int game_time;
  private static Set<UUID> temp_blocked = new HashSet<>();

  public enum State
  {
    STOPPED, PLAYING, ENDING
  };

  public static Game.State getState()
  {
    return Game.state;
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
        Util.sendServerMsg("A game is starting soon!");
        startCountDown();
      }
      if (ZvH.waitersTeam.getEntries().size() == Bukkit.getOnlinePlayers().size())
      {
        if (count_down_time[0] > 4)
          count_down_time[0] = 4;
      }
    } else
    {
      updateBoards();
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
        updateBoards();
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
      Util.playSound(player, Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 0.8f);
      return;
    }
    playing.add(player);
    player.setGameMode(GameMode.SURVIVAL);
    player.setHealth(20d);
    player.getInventory().clear();
    player.setItemOnCursor(null);
    player.clearActivePotionEffects();
    player.setRespawnLocation(
        MapControl.getLocation(player, MapControl.current_size.zombieSpawn(), MapControl.current_size.humanSpawn()),
        true);

    Music.stop(player);
  }

  public static void joinZombies(Player player)
  {
    ZvH.zombiesTeam.addPlayer(player);
    join(player);
    player.teleport(
        MapControl.getLocation(player, MapControl.current_size.zombieSpawn(), MapControl.current_size.humanSpawn()));
    ClassSelectionMenu.showTo(player);
    player.showTitle(zombie_spawn_title);
    Util.playSound(player, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.8f);
  }

  public static void joinHumans(Player player, boolean revived)
  {
    ZvH.humansTeam.addPlayer(player);
    join(player);
    HumanClassManager.giveTo(player);
    player.getInventory().setItem(8, CustomItems.shop_open);

    if (revived)
      return;
    player.teleport(
        MapControl.getLocation(player, MapControl.mapSizes[0].humanSpawn(), MapControl.mapSizes[0].zombieSpawn()));
    player.showTitle(human_spawn_title);
    Util.playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 1f, 0.8f);
  }

  public static void reviveHuman(Player player)
  {
    Util.playSoundAllAt(player, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.9f, 1.2f);
    Util.sendServerMsg(Component.text(player.getName()).decorate(TextDecoration.BOLD)
        .append(Component.text(" has been become human again!").color(NamedTextColor.GRAY))
        .hoverEvent(HoverEvent.showText(Component.text("Infect 3 humans as a zombie"))));

    joinHumans(player, true);
    player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 3, 2));
  }

  public static void leaveHumans(Player player)
  {
    ZvH.humansTeam.removePlayer(player);
    if (getHumansCount() == 1)
    {
      var last_player = getHumans().iterator().next();
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

    if (state == Game.State.PLAYING && playing.remove(player))
    {
      if (playing.size() == 0)
      {
        ZvH.humansTeam.removePlayer(player);
        ZvH.zombiesTeam.removePlayer(player);
        stop();
      } else if (ZvH.humansTeam.removePlayer(player))
      {
        if (Game.getHumansCount() == 0)
        {
          Game.rewardZombies();
          Game.stop();
          Game.announceZombiesWin();
        }
      } else if (ZvH.zombiesTeam.removePlayer(player))
      {
        if (getZombiesCount() == 0)
        {
          // one human
          if (playing.size() == 1)
          {
            stop();
          } else
          {
            if (last_zombies.size() == 1 && last_zombies.iterator().next() == player)
              temp_blocked.add(player.getUniqueId());
            // choose new zombies
            for (var p : pickZombies(playing))
            {
              p.getInventory().clear();
              p.setItemOnCursor(null);
              leaveHumans(p);
              Util.playSound(p, Sound.ENTITY_ZOMBIE_AMBIENT, 50f, 0.8f);
            }
          }
        }
      }
    } else
    {
      ZvH.zombiesTeam.removePlayer(player);
      ZvH.humansTeam.removePlayer(player);
    }

    leaveWaiters(player);
    player.getInventory().clear();
    player.setItemOnCursor(null);
    player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
    player.teleport(ZvH.worldSpawnLocation);

    Music.playLobby(player);
  }

  public static boolean isPlaying(Player player)
  {
    return playing.contains(player);
  }

  public static void start()
  {
    MapControl.chooseMap(ZvH.waitersTeam.getEntries().size());
    MapControl.resetMap();
    ClassSelectionMenu.reset();
    temp_blocked.clear();
    state = Game.State.PLAYING;
    for (var e : ZvH.waitersTeam.getEntries())
    {
      var player = Bukkit.getPlayer(e);
      player.sendActionBar(Component.empty());
      ZvH.waitersTeam.removePlayer(player);
      updateBoard(player);
      playing.add(player);
    }
    game_time = SURIVAL_TIME;// + Math.min(10, playing.size()) * 15;

    for (var player : pickZombies(playing))
    {
      joinZombies(player);
    }

    for (var player : playing)
    {
      if (!ZvH.zombiesTeam.hasPlayer(player))
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
        player.getInventory().addItem(new ItemStack(Material.GRAVEL, 4), new ItemStack(Material.LIGHT_GRAY_WOOL, 2));
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

    Util.sendServerMsg("A game has started.");
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

    ClassSelectionMenu.reset();

    final var style = Style.style(NamedTextColor.WHITE, TextDecoration.BOLD);
    final var killers = Combat.getKillers();
    var killers_msg = Component.text("\n\n  Top killers:");
    for (int i = 0; i < Math.min(killers.size(), 5); i++)
    {
      killers_msg = killers_msg
          .append(Component.text("\n    %-2d ".formatted(killers.get(i).getValue().intValue()), style)
              .append(killers.get(i).getKey().displayName()));
    }

    for (var p : playing)
    {
      Component msg = Component.newline()
          .append(Component.text("Game Summary\n\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.UNDERLINED))
          .append(Component.text("  Your kills: ")).append(Component.text(Combat.getKills(p), style))
          .append(killers_msg);
      msg = msg.appendNewline();
      p.sendMessage(msg);

      leave(p);
    }
    playing.clear();
    Combat.reset();
    ZvH.updateLeaderboards();

    updateBoards();
    Util.sendServerMsg("This game has ended.");
  }

  public static void updateBoards()
  {
    for (var fb : boards.values())
    {
      updateBoard(fb);
    }
  }

  public static void updateBoard(FastBoard fb)
  {
    var lines = new ArrayList<Component>();
    lines.add(Component.empty());
    if (state == Game.State.STOPPED)
    {
      lines.add(Component.text("Waiting... %d/%d".formatted(ZvH.waitersTeam.getEntries().size(), REQUIRED_PLAYERS)));
    } else
    {
      lines.add(getFormatedTime());
    }
    lines.add(Component.empty());
    lines.add(Component.text("Coins: " + ZvH.coins.getScore(fb.getPlayer()).getScore()));
    lines.add(Component.empty());

    fb.updateLines(lines);
  }

  public static void updateBoard(Player player)
  {
    updateBoard(getBoard(player));
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

  private static BukkitTask count_down_task, waiting_task;
  private static Stack<BukkitTask> game_tasks = new Stack<>();
  private static final int[] count_down_time =
  { 0 };

  public static void startCountDown()
  {
    count_down_time[0] = 15 + 1;
    count_down_task = Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      if (--count_down_time[0] == 0)
      {
        cancelCountDown();
        waiting_task.cancel();
        for (var fb : boards.values())
        {
          fb.updateLine(1, Component.text("Working..."));
        }
        start();
        return;
      }
      updateCountDown(count_down_time[0]);
    }, 0, 20);
  }

  public static void cancelCountDown()
  {
    count_down_task.cancel();
    updateBoards();
  }

  private static void updateCountDown(int t)
  {
    for (var fb : boards.values())
    {
      fb.updateLine(1, Component.text("Starting in " + t));
      Util.playSound(fb.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 2f);
    }
  }

  private static Component getFormatedTime()
  {
    return Component.text(String.format("Time: %d:%02d", game_time / 60, game_time % 60));
  }

  private static void updateTime()
  {
    if (--game_time < 0)
    {
      rewardHumans();
      stop();
      announceHumansWin();
      return;
    }
    for (var fb : boards.values())
    {
      fb.updateLine(1, getFormatedTime());
    }
    if (game_time != 0 && game_time % 60 == 0)
    {
      for (var p : getHumans())
      {
        ZvH.changeCoins(p, Rewards.COIN_HUMAN_ALIVE, "stayin alive");
      }
    }
  }

  public static FastBoard addBoard(Player player)
  {
    var fb = new FastBoard(player);
    fb.updateTitle(board_title);
    boards.put(player, fb);
    updateBoard(fb);
    return fb;
  }

  public static void removeBoard(Player player)
  {
    boards.remove(player).delete();
  }

  public static Set<Player> pickZombies(Set<Player> players)
  {
    var options = new HashSet<>(players);
    if (players.size() > 1)
      options.removeAll(last_zombies);
    last_zombies.clear();

    for (int i = 0; i < Math.min((int) Math.ceil((double) players.size() / 6d), 1); i++)
    {
      last_zombies.add(Util.popRandom(options));
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
        Util.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
        player.sendMessage(Component.text("Joined queue").color(NamedTextColor.GREEN));
      }
      break;

    case PLAYING:
      joinZombies(player);
      break;
    }
  }

  public static void announceZombiesWin()
  {
    for (var p : Bukkit.getOnlinePlayers())
    {
      p.showTitle(zombies_win_title);
      Util.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
    }
  }

  public static void rewardZombies()
  {
    for (var p : getZombies())
    {
      if (last_zombies.contains(p))
        ZvH.changeCoins(p, Rewards.COIN_FIRST_ZOMBIE_WIN, "won as first zombie");
      else
        ZvH.changeCoins(p, Rewards.COIN_ZOMBIE_WIN, "won");
    }
  }

  public static void announceHumansWin()
  {
    for (var p : Bukkit.getOnlinePlayers())
    {
      p.showTitle(humans_win_title);
      Util.playSound(p, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
    }
  }

  public static void rewardHumans()
  {
    if (getHumansCount() == 1 && playing.size() > 2)
    {
      ZvH.changeCoins(getHumans().iterator().next(), Rewards.COIN_LAST_HUMAN_WIN, "won as last human");
    } else
      for (var p : getHumans())
      {
        ZvH.changeCoins(p, Rewards.COIN_HUMAN_WIN, "won");
      }
  }

  public static Set<Player> getZombies()
  {
    return Util.getTeamPlayers(ZvH.zombiesTeam);
  }

  public static int getZombiesCount()
  {
    return Util.getTeamPlayersCount(ZvH.zombiesTeam);
  }

  public static Set<Player> getHumans()
  {
    return Util.getTeamPlayers(ZvH.humansTeam);
  }

  public static int getHumansCount()
  {
    return Util.getTeamPlayersCount(ZvH.humansTeam);
  }

  public static void handleDeath(PlayerDeathEvent e)
  {
    if (state == Game.State.ENDING)
    {
      e.setCancelled(true);
      return;
    }
    var player = e.getPlayer();
    DiscordBot.sendMessage(e.deathMessage());
    // kill rewards
    Combat.handleKillRewards(player);

    // last human
    if (ZvH.humansTeam.hasPlayer(player) && Game.getHumansCount() == 1)
    {
      state = Game.State.ENDING;
      final var tm = player.getServer().getServerTickManager();
      // keep player alive
      // TODO: increment stat
      e.setCancelled(true);
      tm.setTickRate(5);
      Game.stopGameTasks();

      ClassSelectionMenu.reset();
      for (var p : Game.playing)
      {
        p.setGameMode(GameMode.ADVENTURE);
        p.closeInventory();
        p.playSound(player, e.getDeathSound(), 1f, 1f);
        p.sendMessage(e.deathMessage());
      }
      Game.rewardZombies();

      Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
        tm.setTickRate(20);
        for (var p : Game.playing)
        {
          p.setGameMode(GameMode.SPECTATOR);
        }
        Bukkit.getScheduler().runTaskLater(ZvH.singleton, () -> {
          Game.stop();
          Game.announceZombiesWin();
        }, 20 * 5);
      }, 20 * 2);
    }
    player.getInventory().clear();
    player.setItemOnCursor(null);
    e.setDroppedExp(0);
    e.setNewTotalExp(0);
  }
}
