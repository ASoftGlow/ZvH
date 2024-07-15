package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public abstract class Cosmetics
{
  public static abstract class Blocks
  {
    public static final byte DEFAULT = 0;
    public static final byte RED = 1;
    public static final byte ORANGE = 2;
    public static final byte YELLOW = 3;
    public static final byte GREEN = 4;
    public static final byte BLUE = 5;
    public static final byte CYAN = 6;
    public static final byte PURPLE = 7;
    public static final byte WHITE = 8;
    public static final byte RAINBOW = 9;

    public static final Material[] fall_materials =
    { //
        Material.GRAVEL, //
        Material.RED_CONCRETE_POWDER, //
        Material.ORANGE_CONCRETE_POWDER, //
        Material.YELLOW_CONCRETE_POWDER, //
        Material.LIME_CONCRETE_POWDER, //
        Material.LIGHT_BLUE_CONCRETE_POWDER, //
        Material.CYAN_CONCRETE_POWDER, //
        Material.PURPLE_CONCRETE_POWDER, //
        Material.WHITE_CONCRETE_POWDER, //
        Material.GRAVEL //
    };
    public static final Material[] solid_materials =
    { //
        Material.LIGHT_GRAY_WOOL, //
        Material.RED_WOOL, //
        Material.ORANGE_WOOL, //
        Material.YELLOW_WOOL, //
        Material.LIME_WOOL, //
        Material.LIGHT_BLUE_WOOL, //
        Material.CYAN_WOOL, //
        Material.PURPLE_WOOL, //
        Material.WHITE_WOOL, //
        Material.LIGHT_GRAY_WOOL //
    };
    public static final NamedTextColor[] text_colors =
    { //
        NamedTextColor.GRAY, //
        NamedTextColor.RED, //
        NamedTextColor.GOLD, //
        NamedTextColor.YELLOW, //
        NamedTextColor.GREEN, //
        NamedTextColor.BLUE, //
        NamedTextColor.DARK_AQUA, //
        NamedTextColor.DARK_PURPLE, //
        NamedTextColor.WHITE, //
        NamedTextColor.WHITE //
    };

    public static boolean isMaterial(Material mats[], Material mat)
    {
      for (int i = 1; i < 9; i++)
      {
        if (mats[i] == mat)
          return true;
      }
      return false;
    }

    public static boolean isMaterial(Material mats[], Item item)
    {
      return isMaterial(mats, item.getItemStack().getType());
    }

    public static Material getSolidMaterialFor(Player player)
    {
      return Blocks.solid_materials[Database.getCachedIntStat(player, "cos_blk").orElse(0)];
    }

    public static Material getFallMaterialFor(Player player)
    {
      return Blocks.fall_materials[Database.getCachedIntStat(player, "cos_blk").orElse(0)];
    }

    public static abstract class PackMasks
    {
      public static final int DEFAULT = 0;
      public static final int COLORFUL = 1 << 0;
      public static final int RAINBOW = 1 << 1;
    }

    public static boolean doesOwnPack(int owned, int mask)
    {
      return mask == 0 ? true : (owned & mask) == mask;
    }
  }

  public static abstract class Particles
  {
    public static final byte DEFAULT = 0;
    public static final byte FLAME_FLY = 1;
    public static final byte SCRAPE = 2;

    public static void start()
    {
      Bukkit.getScheduler().runTaskTimer(ZvH.singleton, bt -> {
        for (var e : Database.cos_particles.entrySet())
        {
          if (e.getKey().isSneaking() || e.getKey().getGameMode() == GameMode.SPECTATOR)
            continue;
          double t = (e.getKey().getTicksLived() % 20) / 20d * Math.PI;

          switch (e.getValue().intValue())
          {
          case Cosmetics.Particles.FLAME_FLY:
            var pos = e.getKey().getLocation().clone().add(Math.sin(t) / 2, Math.sin(t / 3) + 1, Math.cos(t) / 2);
            Particle.FLAME.builder().count(0).offset(0, 0.01, 0).source(e.getKey()).location(pos).receivers(8, true)
                .spawn();
            break;

          case Cosmetics.Particles.SCRAPE:
            Particle.SCRAPE.builder().count(3).source(e.getKey()).location(e.getKey().getLocation().add(0, 0.1, 0))
                .receivers(8, true).spawn();
            break;

          case Cosmetics.Particles.DEFAULT:
          default:
            break;
          }
        }
      }, 0, 2);
    }
  }
}
