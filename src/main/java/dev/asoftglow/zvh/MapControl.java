package dev.asoftglow.zvh;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

@SuppressWarnings("null")
public abstract class MapControl {

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

  public static Location getLocation(World world, Vector pos) {
    return new Location(world, pos.getX(), pos.getY(), pos.getZ());
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
    ZvH.editSession.flushQueue();

    // Circles
    genZSafe(mapSizes[last_size].zombieSpawn, 4);
    genZSafe(mapSizes[last_size].humanSpawn, 6);

    // Spawn protection
    var world = Bukkit.getWorlds().get(0);
    for (var e : world.getEntitiesByClass(Slime.class)) {
      e.remove();
    }

    var slime = (Slime) world.spawnEntity(getLocation(world, mapSizes[last_size].humanSpawn), EntityType.SLIME);
    slime.setSize(10);
    slime.setInvulnerable(true);
    slime.setAI(false);
    slime.setInvisible(true);
    ZvH.zombiesTeam.addEntity(slime);

    slime = (Slime) world.spawnEntity(getLocation(world, mapSizes[last_size].zombieSpawn), EntityType.SLIME);
    slime.setSize(8);
    slime.setInvulnerable(true);
    slime.setAI(false);
    slime.setInvisible(true);
    ZvH.humansTeam.addEntity(slime);
  }

  public static void resetMap(int size) {
    b = mapSizes[size].bounds;

    // Clear building space
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b[0] + 1, b[4] + 1, b[1] + 1),
        BlockVector3.at(b[2] - 1, b[4] + 29, b[3] - 1)),
        BukkitAdapter.asBlockType(Material.AIR).getDefaultState());
    ZvH.editSession.flushQueue();

    if (last_size != size) {
      last_size = size;
      resetBorders();
    }

    var world = Bukkit.getWorlds().get(0);
    for (var e : world.getEntitiesByClasses(Item.class, Arrow.class, TNTPrimed.class, ThrownPotion.class,
        FallingBlock.class)) {
      e.remove();
    }
  }

  // draws a circle, in a block game!
  public static void circle(int x, int y, int z, int radius, Material material, boolean filled, int precision) {

    double angle = 0;
    var world = Bukkit.getWorlds().get(0);

    // Loop through points around the circle
    for (int i = 0; i < precision; i++) {

      // update angle
      angle += 2 * Math.PI / precision;

      // get coords of nearest block
      int dX = x + (int) Math.round(radius * Math.cos(angle));
      int dZ = z + (int) Math.round(radius * Math.sin(angle));

      if (filled == true) {

        // fill in between (dX,dZ) and (x,y)

        fill(x, y, z, dX, y, dZ, material.getKey().asString());

      } else {

        // setBlock at (dX,dZ)
        world.setBlockData(dX, y, dZ, material.createBlockData());
      }
    }
  }

  // generate a zombie safe zone in the map
  public static void genZSafe(Vector pos, int radius) {
    circle(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ(), radius, Material.LIGHT_GRAY_CONCRETE_POWDER, false,
        radius * radius * 3);
  }

  public static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String material) {
    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
        "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2
            + " " + z2 + " " + material);
  }
}