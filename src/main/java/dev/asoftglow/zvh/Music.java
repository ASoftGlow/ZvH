package dev.asoftglow.zvh;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Music {
    
    //Make a map for player toggles
    static Map<Player, String> musicMap = new HashMap<>();

    // turns on music for a specific player using the datapack
    // Current songs:
    // A_Decaying_City
    // Survive_The_Night
    public static void toggle(Player Player, String song){

        if (musicMap.containsKey(Player) == false){
            musicMap.put(Player, "false");
        }

        if (musicMap.get(Player).equals("false")){
            musicMap.put(Player, song);
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "execute as " + Player.getName() + " run function " + song + ":play");
        } else {
            musicMap.put(Player, "false");
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "execute as " + Player.getName() + " run function " + song + ":stop");
        }
    }

    public static void switchSong(Player Player, String song){
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "execute as " + Player.getName() + " run function " + musicMap.get(Player) + ":stop");
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
                "execute as " + Player.getName() + " run function " + song + ":play");
    }
}
