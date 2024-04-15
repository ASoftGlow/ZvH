package dev.asoftglow.zvh;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Player;

public class QOL {
    
    // make dictionary for player afk states
    static Map<Player, Boolean> afkMap = new HashMap<>();

    // Notifies of a player's afk-age
    public static void afkToggle(Player Player){

        // update afkMap
        if (afkMap.containsKey(Player)){
            if(afkMap.get(Player) == true){
                afkMap.put(Player, false);
            } else {
                afkMap.put(Player, true);
            }
        } else {
            afkMap.put(Player, true);
        }
        
        // notify players
        if (afkMap.get(Player) == true){
            Game.sendServerMsg("§l§a" + Player.getName() + " is now afk!");
        } else {
            Game.sendServerMsg("§l§a" + Player.getName() + " is no longer afk!");
        }
        
    }

}
