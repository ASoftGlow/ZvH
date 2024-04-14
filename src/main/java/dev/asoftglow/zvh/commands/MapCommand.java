package dev.asoftglow.zvh.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.asoftglow.zvh.MapControl;
import dev.asoftglow.zvh.ZvH;

public class MapCommand implements CommandExecutor{

    public MapCommand(ZvH zvh) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if the command is executed by a player
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Check if the command is correct
        if (cmd.getName().equalsIgnoreCase("resetMap")) {

            // Actually reset the map
            MapControl.resetMap();
            // Notify player
            player.sendMessage("Map reset!");

            return true;
        }

        return false;
    }
}
