package dev.asoftglow.zvh;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;

import dev.asoftglow.zvh.util.Util;
import dev.asoftglow.zvh.util.WeightedArray;

@SuppressWarnings("null")
public abstract class MapControl {

  public static final class MapFeature implements WeightedArray.Item {
    public final String name;
    public final String description;
    public final Material icon;
    final float weight;

    public float weight() {
      return weight;
    }

    public MapFeature(String name, String description, Material icon, float weight) {
      this.name = name;
      this.description = description;
      this.icon = icon;
      this.weight = weight;
    }
  }

  public static final record MapSize(
      MapBounds bounds,
      int min,
      Vector zombieSpawn,
      Vector humanSpawn) {
  }

  static final class MapBounds {
    public static final int height = 30;
    public final int x1, x2, z1, z2, y;

    public MapBounds(int x1, int z1, int x2, int z2, int y) {
      this.x1 = x1;
      this.x2 = x2;
      this.z1 = z1;
      this.z2 = z2;
      this.y = y;
    }

    public int getXLength() {
      return Math.abs(x2 - x1);
    }

    public int getZLength() {
      return Math.abs(z2 - z1);
    }
  }

  static MapSize current_size = null;
  static MapFeature current_feature = null;

  static MapBounds b;
  static MapSize last_size = null;
  static final ParserContext parserContext = new ParserContext();

  final static MapSize[] mapSizes = {
      new MapSize(new MapBounds(-47, 16, 52, 68, 1), 1, new Vector(40, 2, 42), new Vector(-40, 2, 42))
  };
  public final static MapFeature[] features = new MapFeature[] {
      // new MapFeature("Bridge", "A giant bridge located in the center",
      // Material.OAK_FENCE, 0.1f),
      // new MapFeature("Fortress", "A giant square fortress located in the center",
      // Material.IRON_DOOR, 0.1f),
      new MapFeature("Bars", "Long, square bars randomly spanning the sky", Material.IRON_BARS, 0.2f),
      new MapFeature("Pillars", "Tall, square pillars randomly spread across the map", Material.QUARTZ_PILLAR, 0.2f)
  };
  static final Pattern bars_fp, pillars_fp;
  static {
    parserContext.setActor(BukkitAdapter.adapt(Bukkit.getConsoleSender()));
    bars_fp = WorldEdit.getInstance().getPatternFactory()
        .parseFromInput("60%light_gray_wool,30%gravel,10%cobweb", parserContext);
    pillars_fp = WorldEdit.getInstance().getPatternFactory()
        .parseFromInput("5%light_gray_wool,55%gravel,40%cobweb", parserContext);
  }

  public static void chooseMap(int playerCount) {
    var sizes = new ArrayList<>(Arrays.asList(mapSizes));
    var it = sizes.iterator();
    while (it.hasNext()) {
      var mapSize = it.next();
      if (playerCount < mapSize.min)
        it.remove();
    }
    if (sizes.size() == 0)
      throw new IllegalArgumentException("No maps for such few players");
    current_size = Util.pickRandom(sizes);
    current_feature = WeightedArray.getRandomFrom(features);
    // current_feature = features[1];
    ZvH.singleton.getLogger().info("Feature: " + current_feature.name);
  }

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
        BlockVector3.at(b.x1, b.y - 1, b.z1),
        BlockVector3.at(b.x2, b.y + MapBounds.height, b.z2)).getFaces(),
        BukkitAdapter.asBlockType(Material.BARRIER).getDefaultState());
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b.x1, b.y + MapBounds.height + 1, b.z1),
        BlockVector3.at(b.x2, b.y + MapBounds.height + 2, b.z2)).getWalls(),
        BukkitAdapter.asBlockType(Material.BARRIER).getDefaultState());

    // Floor
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b.x1, b.y, b.z1),
        BlockVector3.at(b.x2, b.y, b.z2)).getWalls(),
        BukkitAdapter.asBlockType(Material.SMOOTH_QUARTZ).getDefaultState());

    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b.x1 + 1, b.y, b.z1 + 1),
        BlockVector3.at(b.x2 - 1, b.y, b.z2 - 1)),
        BukkitAdapter.asBlockType(Material.GRAY_CONCRETE_POWDER).getDefaultState());
    ZvH.editSession.flushQueue();

    // Circles
    genZSafe(last_size.zombieSpawn, 5);
    genZSafe(last_size.humanSpawn, 5);

    // Spawn protection
    for (var e : ZvH.world.getEntitiesByClass(Slime.class)) {
      e.remove();
    }

    var slime = ZvH.world.createEntity(getLocation(ZvH.world, current_size.humanSpawn), Slime.class);
    slime.setSize(10);
    slime.setInvulnerable(true);
    slime.setAI(false);
    slime.setInvisible(true);
    ZvH.zombiesTeam.addEntity(slime);
    slime.spawnAt(slime.getLocation());

    slime = ZvH.world.createEntity(getLocation(ZvH.world, current_size.zombieSpawn), Slime.class);
    slime.setSize(8);
    slime.setInvulnerable(true);
    slime.setAI(false);
    slime.setInvisible(true);
    ZvH.humansTeam.addEntity(slime);
    slime.spawnAt(slime.getLocation());
  }

  static void resetMap() {
    b = current_size.bounds;

    // Clear building space
    ZvH.editSession.setBlocks((Region) new CuboidRegion(
        BlockVector3.at(b.x1 + 1, b.y + 1, b.z1 + 1),
        BlockVector3.at(b.x2 - 1, b.y + 29, b.z2 - 1)),
        BukkitAdapter.asBlockType(Material.AIR).getDefaultState());
    ZvH.editSession.flushQueue();

    if (last_size != current_size) {
      last_size = current_size;
      resetBorders();
    }

    if (current_feature != null) {
      switch (current_feature.name) {
        case "Bars":
          final var bsize = 4;
          int c = ThreadLocalRandom.current().nextInt(2, 5);

          while (c-- != 0) {
            var height = b.y + ThreadLocalRandom.current().nextInt(6, MapBounds.height - 1 - bsize);
            var slide = ThreadLocalRandom.current().nextInt(b.getXLength() / 2 - bsize) + 1;

            ZvH.editSession.setBlocks((Region) new CuboidRegion(
                BlockVector3.at(b.x2 - slide, height, b.z2 - 1),
                BlockVector3.at(b.x2 - slide - bsize, height + bsize, b.z1 + 1)),
                bars_fp);
            ZvH.editSession.setBlocks((Region) new CuboidRegion(
                BlockVector3.at(b.x1 + slide, height, b.z2 - 1),
                BlockVector3.at(b.x1 + slide + bsize, height + bsize, b.z1 + 1)),
                bars_fp);
          }
          break;

        case "Pillars":
          final var psize = (2) - 1;
          int c1 = ThreadLocalRandom.current().nextInt(2, 10);

          while (c1-- != 0) {
            var height = ThreadLocalRandom.current().nextInt(4, MapBounds.height - 8);
            var x_slide = ThreadLocalRandom.current().nextInt(b.getXLength() / 2 - psize);
            var z_slide = ThreadLocalRandom.current().nextInt(b.getZLength() - psize - 1);

            ZvH.editSession.setBlocks((Region) new CuboidRegion(
                BlockVector3.at(b.x2 - x_slide - 1, b.y + 1, b.z1 + z_slide + 1),
                BlockVector3.at(b.x2 - x_slide - psize - 1, b.y + height, b.z1 + z_slide + psize + 1)),
                pillars_fp);
            ZvH.editSession.setBlocks((Region) new CuboidRegion(
                BlockVector3.at(b.x1 + x_slide + 1, b.y + 1, b.z1 + z_slide + 1),
                BlockVector3.at(b.x1 + x_slide + psize + 1, b.y + height, b.z1 + z_slide + psize + 1)),
                pillars_fp);
          }
          break;
      }
      ZvH.editSession.flushQueue();
    }

    for (var e : ZvH.world.getEntitiesByClasses(Item.class, Arrow.class, TNTPrimed.class, ThrownPotion.class,
        FallingBlock.class)) {
      e.remove();
    }
  }

  // draws a circle, in a block game!
  static void drawCircle(int x, int y, int z, int radius, Material material, boolean filled, int precision) {

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

        fill(x, y, z, dX, y, dZ, material.getKey().asString());

      } else {

        // setBlock at (dX,dZ)
        ZvH.world.setBlockData(dX, y, dZ, material.createBlockData());
      }
    }
  }

  // generate a zombie safe zone in the map
  static void genZSafe(Vector pos, int radius) {
    drawCircle(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ(), radius, Material.LIGHT_GRAY_CONCRETE_POWDER,
        false,
        radius * radius * 3);
  }

  public static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String material) {
    Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),
        "fill " + x1 + " " + y1 + " " + z1 + " " + x2 + " " + y2
            + " " + z2 + " " + material);
  }
}