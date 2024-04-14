package dev.asoftglow.zvh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;

public class Game {
  private static final int REQUIRED_PLAYERS = 1;
  private static final int SURIVAL_TIME = 60 * 5;
  private static HashMap<Player, FastBoard> boards = new HashMap<>();
  private static final Style zombie_style = Style.style(NamedTextColor.DARK_GREEN, TextDecoration.BOLD);
  private static final Style human_style = Style.style(NamedTextColor.GOLD, TextDecoration.BOLD);
  private static final TextComponent board_title = Component.text()
      .append(Component.text("Z").style(zombie_style))
      .append(Component.text("v"))
      .append(Component.text("H").style(human_style))
      .build();
  private static final Title zombie_spawn_title = Title.title(
      Component.text("Zombie").style(zombie_style),
      Component.text("Kill the humans!"));
  private static final Title human_spawn_title = Title.title(
      Component.text("Human").style(human_style),
      Component.text("Don't get killed!"));
  private static final Title zombies_win_title = Title.title(
      Component.text("Zombies").style(zombie_style),
      Component.text("have won!"));
  private static final Title humans_win_title = Title.title(
      Component.text("Humans").style(human_style),
      Component.text("have won!"));

  private static Set<Player> last_zombies = new HashSet<>();
  private static Set<Player> playing = new HashSet<>();
  private static boolean active = false;
  private static int game_time;

  public static void init() {

  }

  public static boolean isActive() {
    return active;
  }

  public static void joinWaiters(Player player) {
    ZvH.waitersTeam.addPlayer(player);
    if (ZvH.waitersTeam.getEntries().size() == REQUIRED_PLAYERS) {
      startCountDown();
    } else
      updateBoards();
  }

  public static void leaveWaiters(Player player) {
    if (ZvH.waitersTeam.removePlayer(player))
      if (ZvH.waitersTeam.getEntries().size() == REQUIRED_PLAYERS - 1) {
        cancelCountDown();
      } else
        updateBoards();
  }

  public static void joinSpectators(Player player) {

  }

  public static void leaveSpectators(Player player) {
    player.teleport(ZvH.worldSpawnLocation);
  }

  private static void join(Player player) {
    playing.add(player);
    player.getScoreboardTags().add("playing");
    player.setGameMode(GameMode.SURVIVAL);
    player.getInventory().clear();
    player.setRespawnLocation(MapControl.getLocation(player, MapControl.mapSizes[0].zombieSpawn()), true);
  }

  public static void joinZombies(Player player) {
    ZvH.zombiesTeam.addPlayer(player);
    join(player);
    var loc = MapControl.getLocation(player, MapControl.mapSizes[0].zombieSpawn());
    player.teleport(loc);
    ClassSelectionMenu.showTo(player);
    player.showTitle(zombie_spawn_title);
  }

  public static void joinHumans(Player player) {
    ZvH.humansTeam.addPlayer(player);
    join(player);
    player.getInventory().addItem(new ItemStack(Material.STONE_SWORD));
    var loc = MapControl.getLocation(player, MapControl.mapSizes[0].humanSpawn());
    player.teleport(loc);
    player.showTitle(human_spawn_title);
  }

  public static void leaveHumans(Player player) {
    ZvH.humansTeam.removePlayer(player);
    joinZombies(player);
    if (ZvH.humansTeam.getEntries().size() == 0) {
      zombiesWin();
    }
  }

  public static void leave(Player player) {
    player.getScoreboardTags().remove("playing");
    ZvH.zombiesTeam.removePlayer(player);
    player.clearActivePotionEffects();
    player.setGameMode(GameMode.ADVENTURE);

    if (playing.remove(player)) {
      if (ZvH.humansTeam.hasPlayer(player)) {
        leaveHumans(player);
      } else if (playing.size() == 0) {
        stop();
      } else if (ZvH.zombiesTeam.getEntries().size() == 0) {
        // choose new zombie
        
      }
    } else
      leaveWaiters(player);
    player.getInventory().clear();
    player.teleport(ZvH.worldSpawnLocation);
  }

  public static boolean playerIsPlaying(Player player) {
    return playing.contains(player);
  }

  public static void start() {
    MapControl.resetMap(0);
    active = true;
    game_time = SURIVAL_TIME;
    for (var e : ZvH.waitersTeam.getEntries()) {
      var player = Bukkit.getPlayer(e);
      if (player == null)
        continue;
      ZvH.waitersTeam.removePlayer(player);
      updateBoard(player);
      playing.add(player);
    }

    for (var player : pickZombies(playing)) {
      joinZombies(player);
      Util.playSound(player, Sound.ENTITY_ZOMBIE_AMBIENT, 0.9f, 0.8f);
    }

    for (var player : playing) {
      if (!ZvH.zombiesTeam.hasPlayer(player)) {
        joinHumans(player);
        Util.playSound(player, Sound.ENTITY_VILLAGER_AMBIENT, 0.9f, 0.8f);
      }
    }

    // blocks
    game_tasks.add(Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      for (var e : ZvH.humansTeam.getEntries()) {
        var player = Bukkit.getPlayer(e);
        if (player == null)
          continue;
        player.getInventory().addItem(new ItemStack(Material.GRAVEL, 5),
            new ItemStack(Material.LIGHT_GRAY_WOOL));
      }
    }, 0, 20 * 10));

    // time
    game_tasks.add(Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      updateTime();
    }, 0, 20));
  }

  public static void stop() {
    active = false;
    while (!game_tasks.empty())
      game_tasks.pop().cancel();

    var it = playing.iterator();
    while (it.hasNext())
      leave(it.next());

    updateBoards();
  }

  public static void updateBoards() {
    for (var fb : boards.values()) {
      updateBoard(fb);
    }
  }

  public static void updateBoard(FastBoard fb) {
    var lines = new ArrayList<Component>();
    lines.add(Component.empty());
    if (!isActive()) {
      lines.add(Component.text(
          "Waiting... %d/%d".formatted(
              ZvH.waitersTeam.getEntries().size(), REQUIRED_PLAYERS)));
      lines.add(Component.empty());
    } else {
      lines.add(getFormatedTime());
    }
    lines.add(Component.text("Coins: " + ZvH.coins.getScore(fb.getPlayer()).getScore()));
    lines.add(Component.empty());

    fb.updateLines(lines);
  }

  public static void updateBoard(Player player) {
    updateBoard(boards.get(player));
  }

  private static BukkitTask count_down_tasks;
  private static Stack<BukkitTask> game_tasks = new Stack<>();

  public static void startCountDown() {
    final int[] t = { 10 };
    count_down_tasks = Bukkit.getScheduler().runTaskTimer(ZvH.singleton, () -> {
      if (t[0]-- == 0) {
        cancelCountDown();
        start();
        return;
      }
      updateCountDown(t[0]);
    }, 0, 20);
  }

  public static void cancelCountDown() {
    count_down_tasks.cancel();
    updateBoards();
  }

  private static void updateCountDown(int t) {
    for (var fb : boards.values()) {
      fb.updateLine(1, Component.text("Starting in " + t));
      Util.playSound(fb.getPlayer(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 2f);
    }
  }

  private static Component getFormatedTime() {
    return Component.text(String.format("Time: %d:%02d", game_time / 60, game_time % 60));
  }

  private static void updateTime() {
    if (--game_time < 0) {
      humansWin();
      return;
    }
    for (var fb : boards.values()) {
      fb.updateLine(1, getFormatedTime());
    }
  }

  public static void addBoard(Player player) {
    var fb = new FastBoard(player);
    fb.updateTitle(board_title);
    boards.put(player, fb);
    updateBoard(fb);
  }

  public static void removeBoard(Player player) {
    boards.remove(player).delete();
  }

  public static Set<Player> pickZombies(Set<Player> players) {
    var options = new HashSet<>(players);
    options.removeAll(last_zombies);
    last_zombies.clear();

    for (int i = 0; i < Math.min((int) Math.ceil((double) players.size() / 10d), 1); i++) {
      last_zombies.add(Util.popRandom(options));
    }
    return last_zombies;
  }

  public static void zombiesWin() {
    for (var p : playing) {
      p.showTitle(zombies_win_title);
    }
  }

  public static void humansWin() {
    for (var p : playing) {
      p.showTitle(humans_win_title);
    }
  }
}
