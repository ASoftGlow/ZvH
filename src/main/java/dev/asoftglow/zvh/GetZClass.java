package dev.asoftglow.zvh;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GetZClass {

    //Give a zombie kit to the player
    public static void giveKit(Player p) {

        //Set armor
        ItemStack[] armor = {};

        armor[0] = new ItemStack(Material.LEATHER_BOOTS, 1);
        armor[1] = new ItemStack(Material.LEATHER_LEGGINGS, 1);
        armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
        armor[3] = new ItemStack(Material.LEATHER_HELMET, 1);
 
        p.getInventory().setArmorContents(armor);
    }


}
