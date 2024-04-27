package dev.asoftglow.zvh;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Team;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.commands.MapModifierMenu;
import dev.asoftglow.zvh.commands.MusicCommands;
import dev.asoftglow.zvh.commands.ZvHCommands;
import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import xyz.janboerman.guilib.GuiLibrary;
import xyz.janboerman.guilib.api.GuiListener;

public class ZvH extends JavaPlugin
{
  private GuiListener guiListener;
  public static Team zombiesTeam, humansTeam, waitersTeam;
  public static Location worldSpawnLocation;
  public static Objective coins, zombies_killed, humans_killed, xp, lvl;
  public static ZvH singleton;
  public static EditSession editSession;
  public static World world;
  public static TextDisplay leaderboard;

  public GuiListener getGuiListener()
  {
    return guiListener;
  }

  @Override
  public void onEnable()
  {
    singleton = this;
    var ms = Bukkit.getScoreboardManager().getMainScoreboard();
    zombiesTeam = ms.getTeam("zombies");
    humansTeam = ms.getTeam("humans");
    waitersTeam = ms.getTeam("waiters");
    coins = ms.getObjective("coins");
    xp = ms.getObjective("xp");
    zombies_killed = ms.getObjective("zombies_killed");
    humans_killed = ms.getObjective("humans_killed");
    lvl = ms.getObjective("lvl");

    world = getServer().getWorlds().get(0);
    worldSpawnLocation = world.getSpawnLocation();
    editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));

    Game.clean();
    Bukkit.getScheduler().runTaskLater(this, () -> {
      // hope the server is fully loaded by now
      leaderboard = Util.findEntity("Leaderboard", TextDisplay.class, world);
      updateLeaderboard();
    }, 10);

    ZClassManager.init(getDataFolder().toPath().resolve("classes"), getLogger());
    ZClassManager.registerZClass("Zombie", Material.ZOMBIE_HEAD, 0, null);
    ZClassManager.registerZClass("Skeleton", Material.SKELETON_SKULL, 5, new PotionEffect[]
    { new PotionEffect(PotionEffectType.WEAKNESS, -1, 0) });
    ZClassManager.registerZClass("Blaze", Material.BLAZE_POWDER, 10, null);
    ZClassManager.registerZClass("Witch", Material.POTION, 10, null);
    ZClassManager.registerZClass("Spider", Material.STRING, 10, null);
    ZClassManager.registerZClass("Slime", Material.SLIME_BALL, 5, new PotionEffect[]
    { new PotionEffect(PotionEffectType.JUMP, -1, 2) });

    GuiLibrary guiLibrary = (GuiLibrary) getServer().getPluginManager().getPlugin("GuiLib");
    guiListener = guiLibrary.getGuiListener();

    var csm = new ClassSelectionMenu(this);
    getCommand("zvh").setExecutor(new ZvHCommands());
    getCommand("music").setExecutor(new MusicCommands());
    var pm = getServer().getPluginManager();
    pm.registerEvents(new JoinLeaveListener(), this);
    pm.registerEvents(new MiscListener(), this);
    pm.registerEvents(csm, this);

    saveDefaultConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (sender instanceof Player)
    {
      var player = (Player) sender;
      switch (command.getLabel())
      {
      case "leave":
        Game.leave(player);
        return true;

      case "resetmap":
        MapControl.chooseMap(1);
        MapControl.resetMap();
        return true;

      case "afk":
        QOL.afkToggle(player);
        return true;

      case "shop":
        ShopMenu.handleCommand(player);
        return true;

      case "die":
        player.setHealth(0d);
        return true;

      case "class":
        ClassSelectionMenu.showTo(player);
        return true;

      case "modifier":
        MapModifierMenu.showTo(player);
        return true;

      case "join":
        Game.play(player);
        return true;

      default:
        break;
      }
    }
    return super.onCommand(sender, command, label, args);
  }

  public static void changeCoins(Player player, int amount, String reason)
  {
    if (amount == 0)
      return;
    var s = coins.getScore(player);
    s.setScore(s.getScore() + amount);
    player.sendActionBar(
        Component.text(reason == null ? "%+d coins".formatted(amount) : "%+d coins (%s)".formatted(amount, reason))
            .style(Style.style(NamedTextColor.GOLD)));
    Game.updateBoard(player);
  }

  public static void updateLeaderboard()
  {
    // get scores
    var scores = new ArrayList<Score>();
    for (var entry : Bukkit.getScoreboardManager().getMainScoreboard().getEntries())
    {
      var score = lvl.getScore(entry);
      if (score.isScoreSet())
      {
        scores.add(score);
      }
    }

    // sort scores
    scores.sort((a, b) -> {
      return b.getScore() - a.getScore();
    });

    Component txt = Component.empty().style(Style.style(NamedTextColor.BLACK));
    var c = Math.min(scores.size(), 10);
    for (int i = 0; i < c; i++)
    {
      if (i != 0)
        txt = txt.appendNewline();
      txt = txt.append(Component.text("%-3d %s".formatted(scores.get(i).getScore(), scores.get(i).getEntry())));
    }

    leaderboard.text(txt);
  }
}
