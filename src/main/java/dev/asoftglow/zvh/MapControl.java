package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

@SuppressWarnings("null")
public class MapControl {

  public record MapSize(
      int[] bounds,
      int min,
      Vector zombieSpawn,
      Vector humanSpawn) {
  }

  // stores the two corners of the game arena (x1,z1,x2,z2,y)
  static int[] b;
  private static int last_size = -1;
  final public static MapSize[] mapSizes = {
      new MapSize(new int[] { -47, 16, 52, 68, 1 }, 4, new Vector(40, 2, 42), new Vector(-40, 2, 42))
  };

  public static Location getLocation(Player player, Vector pos, Vector look_pos) {
    var v = pos.clone().subtract(look_pos);
    return new Location(player.getWorld(), pos.getX(), pos.getY(), pos.getZ(), (float) Math.atan2(v.getX(), v.getZ()),
        0f);
  }

  public static void resetBorders() {
    // Walls
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b[0], b[4] - 1, b[1]),
        BlockVector3.at(b[2], b[4] + 30, b[3])).getFaces(),
        BukkitAdapter.asBlockType(Material.BARRIER).getDefaultState());

    // Floor
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b[0], b[4], b[1]),
        BlockVector3.at(b[2], b[4], b[3])).getWalls(),
        BukkitAdapter.asBlockType(Material.SMOOTH_QUARTZ).getDefaultState());

    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b[0] + 1, b[4], b[1] + 1),
        BlockVector3.at(b[2] - 1, b[4], b[3] - 1)),
        BukkitAdapter.asBlockType(Material.GRAY_CONCRETE_POWDER).getDefaultState());
  }

  public static void resetMap(int size) {
    b = mapSizes[size].bounds;

    // Clear building space
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b[0] + 1, b[4] + 1, b[1] + 1),
        BlockVector3.at(b[2] - 1, b[4] + 29, b[3] - 1)),
        BukkitAdapter.asBlockType(Material.AIR).getDefaultState());

    if (last_size != size) {
      last_size = size;
      resetBorders();
      genZSafe(mapSizes[size].zombieSpawn, 4);
    }

    ZvH.editSession.flushQueue();
    var cs = Bukkit.getServer().getConsoleSender();
    Bukkit.getServer().dispatchCommand(cs, "kill @e[type=item]");
    Bukkit.getServer().dispatchCommand(cs, "kill @e[type=#impact_projectiles]");
    Bukkit.getServer().dispatchCommand(cs, "kill @e[type=falling_block]");
    Bukkit.getServer().dispatchCommand(cs, "kill @e[type=potion]");
  }

  // draws a circle, in a block game!
  public static void circle(int x, int y, int z, int radius, String material, boolean filled, int precision) {

    double angle = 0;

    // Loop through points around the circle
    for (int i = 0; i < precision; i++) {

      // update angle
      angle += 2 * Math.PI / precision;

      // get coords of nearest block
      int dX = x + (int) Math.round(radius * Math.cos(angle));
      int dZ = z + (int) Math.round(radius * Math.sin(angle));

      if (filled == true) {

        // fill in between (dX,dZ) and (x,y)
        fill(x, y, z, dX, y, dZ, material);

      } else {

        // setBlock at (dX,dZ)
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
            "setblock " + dX + " " + y + " " + dZ + " minecraft:stone");
      }
    }
  }

  // generate a zombie safe zone in the map
  public static void genZSafe(Vector pos, int radius) {
    circle(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ(), radius, "light_gray_concrete_powder", false,
        radius * radius * 3);
  }

  public static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String material) {
    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
        "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2
            + " " + z2 + " " + material);
  }
}