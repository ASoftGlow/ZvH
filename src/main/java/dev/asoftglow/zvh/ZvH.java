package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;

import dev.asoftglow.zvh.commands.ClassSelectionMenu;
import dev.asoftglow.zvh.commands.MapModifierMenu;
import dev.asoftglow.zvh.commands.MusicCommands;
import dev.asoftglow.zvh.commands.SpeciesClassCommands;
import dev.asoftglow.zvh.util.Util;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import xyz.janboerman.guilib.GuiLibrary;
import xyz.janboerman.guilib.api.GuiListener;

public class ZvH extends JavaPlugin
{
  private GuiListener guiListener;
  public static Team zombiesTeam, humansTeam, waitersTeam;
  public static Location worldSpawnLocation;
  public static ZvH singleton;
  public static EditSession editSession;
  public static World world;
  public static TextDisplay lvlLeaderboard, coinLeaderboard;
  public static Advancement first_blood, first_brains, revival;
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
    Logger.Set(getLogger());
    if (isDev)
      Logger.Get().info("\n\ndev mode enabled\n");

    var ms = Bukkit.getScoreboardManager().getMainScoreboard();
    zombiesTeam = ms.getTeam("zombies");
    humansTeam = ms.getTeam("humans");
    waitersTeam = ms.getTeam("waiters");
    first_brains = Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_brains"));
    first_blood = Bukkit.getAdvancement(NamespacedKey.fromString("zvh:first_blood"));
    revival = Bukkit.getAdvancement(NamespacedKey.fromString("zvh:revival"));

    world = getServer().getWorlds().get(0);
    worldSpawnLocation = world.getSpawnLocation();
    editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world));
    editSession.disableHistory();

    Game.clean();
    // wait until server starts ticking
    Bukkit.getScheduler().runTaskLater(this, () -> {
      lvlLeaderboard = Util.findEntity("LevelLeaderboard", TextDisplay.class, world);
      coinLeaderboard = Util.findEntity("CoinLeaderboard", TextDisplay.class, world);
      updateLeaderboards();
    }, 10);

    Moderation.setConfigFile(getDataFolder().toPath().resolve("muted.json").toFile());
    SpeciesClassManager.init(getDataFolder().toPath().resolve("classes"));

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
    }
    Database.login( //
        getConfig().getString("database.url"), //
        getConfig().getString("database.name"), //
        getConfig().getString("database.username"), //
        getConfig().getString("database.password") //
    );

    GuiLibrary guiLibrary = (GuiLibrary) getServer().getPluginManager().getPlugin("GuiLib");
    guiListener = guiLibrary.getGuiListener();

    getCommand("zvh").setExecutor(new SpeciesClassCommands());
    getCommand("music").setExecutor(new MusicCommands());
    final var pm = getServer().getPluginManager();
    pm.registerEvents(new JoinLeaveListener(), this);
    pm.registerEvents(new MiscListener(), this);
    pm.registerEvents(new GuardListener(), this);
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
    Database.logout();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    switch (command.getLabel())
    {
    case "start":
      Game.start();
      sender.sendMessage("Started.");
      break;

    case "stop":
      Game.stop();
      sender.sendMessage("Stopped.");
      break;

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

    case "rules":
      sender.sendMessage("Rules:\n\n" + //
          "- Be respectful and kind\n" + //
          "- No NSFW content\n" + //
          "- Stay on-topic\n" + //
          "- Don't ask for roles or coins\n" + //
          "- Don't spam\n" + //
          "- Don't use cheats\n" + //
          "- Don't farm coins/levels\n" + //
          "- No team griefing");
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

        case "discord":
          player.sendMessage(Component.text("Click to visit").clickEvent(ClickEvent.openUrl(discordLink)));
          return true;

        case "vanish":
          player.sendMessage("Whoosh!");
          if (args.length == 1 && player.isOp())
          {
            player = Bukkit.getPlayerExact(args[0]);
            if (player == null)
              return false;
          }
          Moderation.vanish(player);
          return true;

        case "stats":
          var target = player;
          if (args.length == 1)
          {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null)
              return false;
          }
          final var pl = player;
          final var tar = target;
          Database.fetchPlayerStats(target, stats -> {
            Component page = Component.empty()
                .append(Component.text(tar.getName() + "'s Stats\n").decorate(TextDecoration.UNDERLINED));
            for (var p : stats.entrySet())
            {
              page = page.append(Component.text("\n" + p.getKey(), NamedTextColor.GRAY))
                  .append(Component.text(": " + p.getValue()));
            }
            pl.openBook(Book.book(Component.text("Stats"), Component.text("ASoftGlow"), page));
          });
          return true;

        default:
          break;
        }
        break;
      }
    }
    return super.onCommand(sender, command, label, args);
  }

  public static void updateLeaderboards()
  {
    updateLeaderboard("lvl", lvlLeaderboard);
    updateLeaderboard("coins", coinLeaderboard);
  }

  private static void updateLeaderboard(String stat, TextDisplay textDisplay)
  {
    Database.getIntStatLeaderboard(stat, 10, players -> {
      var sb = new StringBuilder();

      players.forEach((p, n) -> {
        sb.append("\n%-3d %s".formatted(n.intValue(), p.getName()));
      });
      sb.deleteCharAt(0);

      textDisplay.text(Component.text(sb.toString(), NamedTextColor.BLACK));
    });
  }

  public static int getPlayerCount()
  {
    return Bukkit.getOnlinePlayers().size();
  }
}
