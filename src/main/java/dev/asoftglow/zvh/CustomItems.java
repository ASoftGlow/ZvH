package dev.asoftglow.zvh;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.janboerman.guilib.api.ItemBuilder;

public abstract class CustomItems
{
    public static final ItemStack tracker = new ItemBuilder(Material.COMPASS).name("Tracker")
            .lore("Points to humans by detecting their heat signature").build();
    public static final ItemStack shop_open = new ItemBuilder(Material.BOOK).name("§rOpen Shop")
            .lore("Right click to use").build();
    public static final ItemStack spec_leave = new ItemBuilder(Material.BARRIER).name("§r§6Click to leave").build();
    public static final ItemStack loading = new ItemBuilder(Material.REDSTONE_TORCH).name("§8Loading...")
            .flags(ItemFlag.HIDE_ATTRIBUTES).build();
    public static final ItemStack light_fuse = new ItemStack(Material.RED_DYE);
    static
    {
        var meta = light_fuse.getItemMeta();
        meta.displayName(Component.text("Light Fuse", NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("Right click", NamedTextColor.GRAY)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        light_fuse.setItemMeta(meta);
    }

    public static final Material[] fall_blocks =
    {
        Material.GRAVEL,
        Material.RED_CONCRETE_POWDER,
        Material.ORANGE_CONCRETE_POWDER,
        Material.YELLOW_CONCRETE_POWDER,
        Material.LIME_CONCRETE_POWDER,
        Material.LIGHT_BLUE_CONCRETE_POWDER,
        Material.CYAN_CONCRETE_POWDER,
        Material.PURPLE_CONCRETE_POWDER,
        Material.WHITE_CONCRETE_POWDER
    };
    public static final Material[] solid_blocks =
    {
        Material.LIGHT_GRAY_WOOL,
        Material.RED_WOOL,
        Material.ORANGE_WOOL,
        Material.YELLOW_WOOL,
        Material.LIME_WOOL,
        Material.LIGHT_BLUE_WOOL,
        Material.CYAN_WOOL,
        Material.PURPLE_WOOL,
        Material.WHITE_WOOL
    };
}
