package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Team;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.commands.ZvHCommands;
import xyz.janboerman.guilib.GuiLibrary;
import xyz.janboerman.guilib.api.GuiListener;

public class ZvH extends JavaPlugin {

  private GuiListener guiListener;
  public static Team zombiesTeam, humansTeam, waitersTeam;
  public static Location worldSpawnLocation;
  public static Objective coins, zombies_killed, humans_killed, xp;
  public static ZvH singleton;
  public static EditSession editSession;

  public GuiListener getGuiListener() {
    return guiListener;
  }

  @Override
  public void onEnable() {
    singleton = this;
    var ms = Bukkit.getScoreboardManager().getMainScoreboard();
    zombiesTeam = ms.getTeam("zombies");
    humansTeam = ms.getTeam("humans");
    waitersTeam = ms.getTeam("waiters");
    coins = ms.getObjective("coins");
    xp = ms.getObjective("xp");
    zombies_killed = ms.getObjective("zombies_killed");
    humans_killed = ms.getObjective("humans_killed");

    var world = getServer().getWorlds().get(0);
    worldSpawnLocation = world.getSpawnLocation();
    editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));

    ZClassManager.init(getDataFolder().toPath().resolve("classes"), getLogger());
    ZClassManager.registerZClass("Zombie", Material.ZOMBIE_HEAD, 0, null);
    ZClassManager.registerZClass("Skeleton", Material.SKELETON_SKULL, 10, null);
    ZClassManager.registerZClass("Blaze", Material.BLAZE_POWDER, 10, null);
    ZClassManager.registerZClass("Witch", Material.POTION, 10, null);
    ZClassManager.registerZClass("Spider", Material.STRING, 10, null);
    ZClassManager.registerZClass("Slime", Material.SLIME_BALL, 10, new PotionEffect[] {
        new PotionEffect(PotionEffectType.JUMP, -1, 2)
    });

    GuiLibrary guiLibrary = (GuiLibrary) getServer().getPluginManager().getPlugin("GuiLib");
    guiListener = guiLibrary.getGuiListener();

    var csm = new ClassSelectionMenu(this);
    getCommand("class").setExecutor(csm);
    getCommand("zvh").setExecutor(new ZvHCommands());
    var pm = getServer().getPluginManager();
    pm.registerEvents(new JoinLeaveListener(), this);
    pm.registerEvents(new MiscListener(), this);
    pm.registerEvents(csm, this);

    saveDefaultConfig();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (sender instanceof Player) {
      var player = (Player) sender;
      switch (command.getLabel()) {
        case "leave":
          Game.leave(player);
          return true;

        case "resetmap":
          MapControl.resetMap(0);
          return true;

        case "afk":
          QOL.afkToggle(player);
          return true;

        default:
          break;
      }
    }
    return super.onCommand(sender, command, label, args);
  }
}
