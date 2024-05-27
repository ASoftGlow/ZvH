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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
  public static TextDisplay lvlLeaderboard, coinLeaderboard;
  public static final boolean isDev = false;
  /**
   * Not thread safe!
   */
  public static String CMD = null;

  public static final String discordLink = "https://discord.gg/mzj4EvBbhM";
  public static final PlainTextComponentSerializer plainSerializer = PlainTextComponentSerializer.plainText();
  private static final TextComponent discordMsgPrefix = Component.text("[Discord]").decorate(TextDecoration.BOLD)
      .hoverEvent(HoverEvent.showText(Component.text("Sent from Discord"))).color(NamedTextColor.BLUE);
  public static final TextComponent reminderMsg = Component.text("Remember to read the ")
      .append(Component.text("guide").decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.runCommand("/guide"))
          .hoverEvent(HoverEvent.showText(Component.text("Click to view"))))
      .append(Component.text(" and the ")).append(Component.text("rules").decorate(TextDecoration.UNDERLINED)
          .clickEvent(ClickEvent.runCommand("/rules")).hoverEvent(HoverEvent.showText(Component.text("Click to view"))))
      .append(Component.text("!"));

  public GuiListener getGuiListener()
  {
    return guiListener;
  }

  @Override
  public void onEnable()
  {
    singleton = this;
    if (isDev)
      getLogger().info("\n\ndev mode enabled\n");

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
    editSession.disableHistory();

    Game.clean();
    Bukkit.getScheduler().runTaskLater(this, () -> {
      // hope the server is fully loaded by now
      lvlLeaderboard = Util.findEntity("LevelLeaderboard", TextDisplay.class, world);
      coinLeaderboard = Util.findEntity("CoinLeaderboard", TextDisplay.class, world);
      updateLeaderboards();
    }, 10);

    Moderation.setConfigFile(getDataFolder().toPath().resolve("muted.json").toFile());
    ZClassManager.init(getDataFolder().toPath().resolve("classes"), getLogger());
    if (!isDev)
    {
      DiscordBot.login(getConfig().getString("discord.bot-token"));
      DiscordBot.setMessageHandler((author, message) -> {
        for (var p : Bukkit.getOnlinePlayers())
        {
          p.sendMessage(Component.empty().append(discordMsgPrefix).append(Component.text(" <" + author + "> "))
              .append(Component.text(message)));
        }
      });
    } else
    {
      // Database.login(getConfig().getString("database.url"),
      // getConfig().getString("database.username"),
      // getConfig().getString("database.password"));
    }

    ZClassManager.registerZClass("Zombie", Material.ZOMBIE_HEAD, 0);
    ZClassManager.registerZClass("Baby_Zombie", Material.CARROT, 4,
        new PotionEffect(PotionEffectType.SPEED, -1, 0, false, false, false),
        new PotionEffect(PotionEffectType.FAST_DIGGING, -1, 0));
    ZClassManager.registerZClass("Skeleton", Material.SKELETON_SKULL, 5,
        new PotionEffect(PotionEffectType.WEAKNESS, -1, 0));
    ZClassManager.registerZClass("Slime", Material.SLIME_BALL, 6,
        new PotionEffect(PotionEffectType.JUMP, -1, 2, false, false, false));
    ZClassManager.registerZClass("Witch", Material.POTION, 8);
    ZClassManager.registerZClass("Spider", Material.STRING, 10);
    ZClassManager.registerZClass("Blaze", Material.BLAZE_POWDER, 15);

    GuiLibrary guiLibrary = (GuiLibrary) getServer().getPluginManager().getPlugin("GuiLib");
    guiListener = guiLibrary.getGuiListener();

    var csm = new ClassSelectionMenu(this);
    getCommand("zvh").setExecutor(new ZvHCommands());
    getCommand("music").setExecutor(new MusicCommands());
    final var pm = getServer().getPluginManager();
    pm.registerEvents(new JoinLeaveListener(), this);
    pm.registerEvents(new MiscListener(), this);
    pm.registerEvents(new GuardListener(), this);
    pm.registerEvents(csm, this);
    pm.registerEvents(new Moderation(), this);

    Bukkit.getScheduler().runTaskTimer(this, () -> {
      Util.sendServerMsg(reminderMsg);
    }, 40, 20 * 60 * 10);
    Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
      if (CMD != null)
      {
        getServer().dispatchCommand(getServer().getConsoleSender(), CMD);
        CMD = null;
      }
    }, 40, 1);

    saveDefaultConfig();
  }

  @Override
  public void onDisable()
  {
    if (Game.getState() != Game.State.STOPPED)
      Game.stop();
    DiscordBot.stop();
    // Database.logout();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    switch (command.getLabel())
    {
    case "resetmap":
      MapControl.chooseMap(1);
      if (args.length == 1)
      {
        try
        {
          MapControl.setFeature(Integer.parseInt(args[0]));
        } catch (NumberFormatException e)
        {
          return false;
        }
      }
      MapControl.resetMap();
      return true;

    case "mute":
    {
      if (args.length != 1)
        return false;
      var p = Bukkit.getPlayerExact(args[0]);
      if (p == null)
        return false;
      Moderation.mute(p);
      return true;
    }

    case "unmute":
    {
      if (args.length != 1)
        return false;
      var p = Bukkit.getPlayerExact(args[0]);
      if (p == null)
        return false;
      Moderation.unmute(p);
      return true;
    }

    case "mutelist":
      var players = Moderation.getMuted();
      if (players.length == 0)
      {

        sender.sendMessage("*Empty*");
        return true;
      }
      var names = new StringBuilder();
      for (int i = 0; i < players.length; i++)
      {
        names.append(players[i].getName());
        if (i != players.length - 1)
          names.append(", ");
      }
      sender.sendMessage(names.toString());
      return true;

    default:
      if (sender instanceof Player)
      {
        var player = (Player) sender;
        switch (command.getLabel())
        {
        case "leave":
          if (args.length == 1 && player.isOp())
          {
            player = Bukkit.getPlayerExact(args[0]);
            if (player == null)
              return false;
          }
          Game.leave(player);
          return true;

        case "shop":
          ShopMenu.handleCommand(player);
          return true;

        case "die":
          player.setHealth(0d);
          return true;

        case "class":
          if (args.length == 1)
          {
            player = Bukkit.getPlayerExact(args[0]);
            if (player == null)
              return false;
          }
          ClassSelectionMenu.showTo(player);
          return true;

        case "modifier":
          MapModifierMenu.showTo(player);
          return true;

        case "join":
          if (args.length == 1 && player.isOp())
          {
            player = Bukkit.getPlayerExact(args[0]);
            if (player == null)
              return false;
          }
          Game.play(player);
          return true;

        case "guide":
          GuideBook.showTo(player);
          return true;

        case "rules":
          player.sendMessage("Rules:\n\n" + //
              "- Be respectful and kind\n" + //
              "- No NSFW content\n" + //
              "- Stay on-topic\n" + //
              "- Don't ask for roles or coins\n" + //
              "- Don't spam\n" + //
              "- Don't use cheats\n" + //
              "- Don't farm coins/levels\n" + //
              "- No team griefing");
          return true;

        case "discord":
          player.sendMessage(Component.text("Click to open").clickEvent(ClickEvent.openUrl(discordLink)));
          return true;

        case "vanish":
          player.sendMessage("Whoosh!");
          return true;

        default:
          break;
        }
        break;
      }
    }
    return super.onCommand(sender, command, label, args);
  }

  public static void changeCoins(Player player, int amount)
  {
    changeCoins(player, amount, null);
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

  public static void changeExp(Player player, int amount)
  {
    var s = ZvH.xp.getScore(player);
    var ns = s.getScore() + amount;
    s.setScore(ns);
    var lvl = ZvH.lvl.getScore(player);
    player.setExp(0);
    player.setLevel(0);
    player.giveExp(ns, false);
    if (lvl.getScore() != player.getLevel())
    {
      lvl.setScore(player.getLevel());
    }
  }

  public static void updateLeaderboards()
  {
    updateLeaderboard(lvl, lvlLeaderboard);
    updateLeaderboard(coins, coinLeaderboard);
  }

  private static void updateLeaderboard(Objective objective, TextDisplay textDisplay)
  {
    // get scores
    var scores = new ArrayList<Score>();
    for (var entry : Bukkit.getScoreboardManager().getMainScoreboard().getEntries())
    {
      var score = objective.getScore(entry);
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

    textDisplay.text(txt);
  }

  public static int getPlayerCount()
  {
    return Bukkit.getOnlinePlayers().size();
  }
}
