package dev.asoftglow.zvh.commands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.asoftglow.zvh.Game;
import dev.asoftglow.zvh.ZClassManager;

public class ZvHCommands implements CommandExecutor, TabCompleter
{
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (args.length == 0 || !(sender instanceof Player))
      return false;
    final var player = (Player) sender;
    switch (args[0])
    {
    case "kit":
      if (args.length < 3)
        return false;
      String name = args[3];
      switch (args[1])
      {
      case "zombie":
      case "z":
        if (args.length < 4)
          return false;

        switch (args[2])
        {
        case "save":
        case "s":
          if (ZClassManager.zClasses.containsKey(name))
          {
            ZClassManager.zClasses.get(name).items = player.getInventory().getContents();
          } else
          {
            player.sendMessage("Class %s doesn't exist!".formatted(name));
            break;
          }

          ZClassManager.saveZClassFrom(name, player.getInventory());
          player.sendMessage("Saved class %s.".formatted(name));
          break;

        case "give":
        case "g":
          var zClass = ZClassManager.zClasses.get(name);
          if (zClass == null)
          {
            player.sendMessage("Class %s doesn't exist!".formatted(name));
            break;
          }
          zClass.give(player);
          player.sendMessage("Gave class %s.".formatted(name));
          break;

        default:
          break;
        }
        break;

      case "human":
      case "h":
        break;

      default:
        return false;
      }
      break;

    case "start":
      Game.start();
      player.sendMessage("Started.");
      break;

    case "stop":
      Game.stop();
      player.sendMessage("Stopped.");
      break;

    default:
      return false;
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
      @NotNull String label, @NotNull String[] args)
  {
    if (args.length == 1)
    {
      return List.of("kit", "start", "stop");
    }
    switch (args[0])
    {
    case "kit":
      if (args.length == 2)
      {
        return List.of("zombie", "human");
      }
      switch (args[1])
      {
      case "zombie":
      case "z":
        if (args.length == 3)
        {
          return List.of("save", "give", "delete");
        }
        if (args.length == 4)
        {
          return List.copyOf(ZClassManager.zClasses.keySet());
        }
        break;

      case "human":
      case "h":
        if (args.length == 3)
        {
          return List.of("save", "reset");
        }
        break;

      default:
        break;
      }
      break;

    default:
      break;
    }

    return null;
  }

}
