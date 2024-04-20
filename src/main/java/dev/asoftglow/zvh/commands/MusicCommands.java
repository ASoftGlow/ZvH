package dev.asoftglow.zvh.commands;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.asoftglow.zvh.Music;

public class MusicCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
        String[] args) {
      if (args.length == 0 || !(sender instanceof Player))
        return false;
      final var player = (Player) sender;
      switch (args[0]) {
        case "play":
          if (args.length < 2)
            return false;
          switch (args[1]) {
            case "a_decaying_city":
            case "adc":
              Music.toggle(player, "A_Decaying_City");
              player.sendMessage("Now Playing: A Decaying City");
              break;
            case "survive_the_night":
            case "stn":
              Music.toggle(player, "Survive_The_Night");
              player.sendMessage("Now Playing: Survive The Night");
              break;
          }
          break;
        case "stop":
          Music.toggle(player, "doesn't matter");
          player.sendMessage("Now Stopping All Music");
          break;
        default:
          return false;
      }
      return true;
    }
  
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
        @NotNull String label, @NotNull String[] args) {
      if (args.length == 1) {
        return List.of("play", "stop");
      }
      switch (args[0]) {
        case "play":
          if (args.length == 2) {
            return List.of("a_decaying_city", "survive_the_night");
          }
          break;
  
        default:
          break;
      }
  
      return null;
    }  
}
