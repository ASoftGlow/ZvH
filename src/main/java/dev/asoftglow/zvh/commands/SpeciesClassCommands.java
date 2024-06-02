package dev.asoftglow.zvh.commands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.asoftglow.zvh.SpeciesClassManager;

public class SpeciesClassCommands implements CommandExecutor, TabCompleter
{
  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
  {
    if (args.length == 0 || !(sender instanceof Player))
      return false;
    if (!(sender instanceof Player))
      return false;
    final var player = (Player) sender;
    switch (args[0])
    {
    case "kit":
      if (args.length < 3)
        return false;
      String name = args[3];
      String species = args[1];
      switch (species)
      {
      case "zombie":
      case "human":
        if (args.length < 4)
          return false;

        switch (args[2])
        {
        case "save":
          if (SpeciesClassManager.speciesHasClass(species, name))
          {
            SpeciesClassManager.speciesGetClass(species, name).setItems(player.getInventory().getContents());
          } else
          {
            player.sendMessage("Class %s doesn't exist!".formatted(name));
            break;
          }

          SpeciesClassManager.saveClassFrom(name, species, player.getInventory());
          player.sendMessage("Saved class %s.".formatted(name));
          break;

        case "give":
          var s_class = SpeciesClassManager.speciesGetClass(species, name);
          if (s_class == null)
          {
            player.sendMessage("Class %s doesn't exist!".formatted(name));
            break;
          }
          s_class.giveTo(player);
          player.sendMessage("Gave class %s.".formatted(name));
          break;

        default:
          break;
        }
        break;

      default:
        return false;
      }
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
      var species = args[1];
      switch (species)
      {
      case "zombie":
      case "human":
        if (args.length == 3)
        {
          return List.of("save", "give", "delete");
        }
        if (args.length == 4)
        {
          return SpeciesClassManager.speciesGetClassNames(species);
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
