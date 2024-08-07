package dev.asoftglow.zvh;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.IllegalArgumentException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;

import lombok.Getter;
import dev.asoftglow.zvh.menus.MapModifierMenu.MapModifier;
import dev.asoftglow.zvh.util.Logger;
import dev.asoftglow.zvh.util.WeightedArray;

@SuppressWarnings("null")
public abstract class MapControl
{
  // #region Subclasses
  public static final class MapFeature implements WeightedArray.Item, MapModifier
  {
    @Getter
    public final String name;
    @Getter
    public final String description;
    @Getter
    public final Material icon;
    @Getter
    final float weight;
    @Getter
    final int min_players;
    final int ceiling;

    public String getType()
    {
      return "Feature";
    }

    public MapFeature(String name, String description, Material icon, float weight, int min_players,
        int ceiling_override)
    {
      this.name = name;
      this.description = description;
      this.icon = icon;
      this.weight = weight;
      this.min_players = min_players;
      this.ceiling = ceiling_override;
    }

    public MapFeature(String name, String description, Material icon, float weight)
    {
      this(name, description, icon, weight, 0);
    }

    public MapFeature(String name, String description, Material icon, float weight, int min_players)
    {
      this(name, description, icon, weight, min_players, 0);
    }
  }

  public static final record MapSize(MapBounds bounds, Vector zombieSpawn, Vector humanSpawn)
  {
  }

  static final class MapBounds extends CuboidRegion
  {
    public final int x1, x2, z1, z2, y, h;

    public MapBounds(int x1, int z1, int x2, int z2, int y, int h)
    {
      super(BlockVector3.at(x1, y, z1), BlockVector3.at(x2, y + h, z2));
      this.x1 = x1;
      this.x2 = x2;
      this.z1 = z1;
      this.z2 = z2;
      this.y = y;
      this.h = h;
    }

    public boolean contains(Block block)
    {
      return this.contains(block.getX(), block.getY(), block.getZ());
    }
  }
  // #endregion

  static MapSize current_size = null;
  public static MapFeature current_feature = null;

  private static MapBounds b;
  static int last_bounds_hash = -1;

  // #region Maps
  private final static RangeMap<Integer, MapSize> mapSizes = TreeRangeMap.create();
  static
  {
    mapSizes.put(Range.closed(1, 3),
        new MapSize(new MapBounds(-32, 16, 37, 68, 1, 30), new Vector(27, 2, 42), new Vector(-22, 2, 42)));
    mapSizes.put(Range.greaterThan(3),
        new MapSize(new MapBounds(-47, 16, 52, 68, 1, 30), new Vector(42, 2, 42), new Vector(-37, 2, 42)));
  }
  public final static MapFeature[] features = new MapFeature[]
  { //
      new MapFeature("Bridge", "A giant bridge located in the center", Material.LADDER, 0.15f, 3),
      new MapFeature("Fortress", "A giant fortress located in the center", Material.IRON_DOOR, 0.1f, 0, 10),
      new MapFeature("Bars", "Long, square bars randomly spanning the sky", Material.IRON_BARS, 0.2f),
      new MapFeature("Pillars", "Tall, square pillars randomly spread across the map", Material.QUARTZ_PILLAR, 0.25f),
      new MapFeature("Grid", "A grid spanning across the sky", Material.OAK_TRAPDOOR, 0.15f, 3, 20),
      new MapFeature("Scatter", "Random blocks in the air", Material.MELON_SEEDS, 0.12f),
      new MapFeature("Nothing", "The default plane of existence", Material.STRUCTURE_VOID, 0.3f)
      /**/ };
  static final File fortress_schem = ZvH.singleton.getDataFolder().toPath().resolve("schematics/fort.schem").toFile();
  static final File bridge_schem = ZvH.singleton.getDataFolder().toPath().resolve("schematics/bridge.schem").toFile();
  static final Pattern bars_p, pillars_p, fortress_p, bridge_p1, bridge_p2, grid_p, scatter_p;
  static final Set<BaseBlock> red_filter, orange_filter;
  static
  {
    final var parserContext = new ParserContext();
    parserContext.setActor(BukkitAdapter.adapt(Bukkit.getConsoleSender()));
    final var pf = WorldEdit.getInstance().getPatternFactory();

    bars_p = pf.parseFromInput("60%light_gray_wool,30%gravel,10%cobweb", parserContext);
    pillars_p = pf.parseFromInput("5%light_gray_wool,55%gravel,40%cobweb", parserContext);
    fortress_p = pf.parseFromInput("75%light_gray_wool,25%gravel", parserContext);
    bridge_p1 = pf.parseFromInput("50%light_gray_wool,50%gravel", parserContext);
    bridge_p2 = pf.parseFromInput("90%light_gray_wool,10%gravel", parserContext);
    grid_p = pf.parseFromInput("70%light_gray_wool,20%gravel,10%air", parserContext);
    scatter_p = pf.parseFromInput("2%light_gray_wool,98%air", parserContext);

    red_filter = new HashSet<BaseBlock>();
    red_filter.add(BukkitAdapter.adapt(Material.RED_WOOL.createBlockData()).toBaseBlock());
    orange_filter = new HashSet<BaseBlock>();
    orange_filter.add(BukkitAdapter.adapt(Material.ORANGE_WOOL.createBlockData()).toBaseBlock());
  }
  // #endregion

  public static List<MapFeature> getFeatures(int playerCount)
  {
    var feats = new ArrayList<>(Arrays.asList(features));
    var fit = feats.iterator();
    while (fit.hasNext())
    {
      if (playerCount < fit.next().min_players)
        fit.remove();
    }
    return feats;
  }

  public static void chooseMapSize(int playerCount)
  {
    current_size = mapSizes.get(playerCount);
    if (current_size == null)
    {
      throw new IllegalArgumentException("No map size for given player count");
    }
  }

  public static void chooseMap(int playerCount)
  {
    chooseMapSize(playerCount);
    current_feature = WeightedArray.getRandomFrom(getFeatures(playerCount));
    if (current_feature.name == "Nothing")
      current_feature = null;
    else
      Logger.Get().info("Feature: " + current_feature.name);
  }

  public static void setFeature(int index)
  {
    if (index < 0 || index > features.length - 1)
      return;
    current_feature = features[index];
  }

  public static Location getLocation(Player player, Vector pos, Vector look_pos)
  {
    return new Location(player.getWorld(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
        (float) Math.toDegrees(Math.atan2(pos.getX() - look_pos.getX(), pos.getZ() - look_pos.getZ())), 0);
  }

  public static Location getLocation(World world, Vector pos)
  {
    return new Location(world, pos.getX(), pos.getY(), pos.getZ());
  }

  private static void resetSpawns()
  {
    final var zombie_center = BlockVector3.at(current_size.zombieSpawn.getBlockX(),
        current_size.zombieSpawn.getBlockY(), current_size.zombieSpawn.getBlockZ());
    ZvH.editSession.setBlocks((Region) new CylinderRegion(zombie_center, Vector2.at(3, 3), b.y + 1, b.y + 4),
        BukkitAdapter.asBlockType(Material.AIR));

    final var human_center = BlockVector3.at(current_size.humanSpawn.getBlockX(), current_size.humanSpawn.getBlockY(),
        current_size.humanSpawn.getBlockZ());
    ZvH.editSession.setBlocks((Region) new CylinderRegion(human_center, Vector2.at(3, 3), b.y + 1, b.y + 4),
        BukkitAdapter.asBlockType(Material.AIR));
  }

  public static void resetBorders()
  {
    // Walls
    ZvH.editSession.setBlocks(
        (Region) new CuboidRegion(BlockVector3.at(b.x1, b.y - 1, b.z1), BlockVector3.at(b.x2, b.y + b.h, b.z2))
            .getFaces(),
        BukkitAdapter.asBlockType(Material.BARRIER).getDefaultState());
    ZvH.editSession.setBlocks(
        (Region) new CuboidRegion(BlockVector3.at(b.x1, b.y + b.h + 1, b.z1),
            BlockVector3.at(b.x2, b.y + b.h + 2, b.z2)).getWalls(),
        BukkitAdapter.asBlockType(Material.BARRIER).getDefaultState());

    // Floor
    ZvH.editSession.setBlocks(
        (Region) new CuboidRegion(BlockVector3.at(b.x1, b.y, b.z1), BlockVector3.at(b.x2, b.y, b.z2)).getWalls(),
        BukkitAdapter.asBlockType(Material.SMOOTH_QUARTZ).getDefaultState());

    ZvH.editSession.setBlocks(
        (Region) new CuboidRegion(BlockVector3.at(b.x1 + 1, b.y, b.z1 + 1), BlockVector3.at(b.x2 - 1, b.y, b.z2 - 1)),
        BukkitAdapter.asBlockType(Material.GRAY_CONCRETE_POWDER).getDefaultState());
    ZvH.editSession.flushQueue();

    // Circles
    genZSafe(current_size.zombieSpawn, 5);
    genZSafe(current_size.humanSpawn, 5);
  }

  private static void clearBuildingSpace()
  {
    ZvH.editSession.setBlocks(
        (Region) new CuboidRegion(BlockVector3.at(b.x1 + 1, b.y + 1, b.z1 + 1),
            BlockVector3.at(b.x2 - 1, b.y + b.h - 1, b.z2 - 1)),
        BukkitAdapter.asBlockType(Material.AIR).getDefaultState());
  }

  static void resetMap()
  {
    var current_bounds_hash = getBoundsHash(current_size, current_feature);
    if (b != null && last_bounds_hash != current_bounds_hash)
    {
      // clear past junk
      clearBuildingSpace();

      ZvH.editSession.setBlocks(
          (Region) new CuboidRegion(BlockVector3.at(b.x1 + 1, b.y, b.z1 + 1), BlockVector3.at(b.x2 - 1, b.y, b.z2 - 1)),
          BukkitAdapter.asBlockType(Material.AIR).getDefaultState());
    }

    b = current_size.bounds;
    if (current_feature != null && current_feature.ceiling > 0)
    {
      b = new MapBounds(b.x1, b.z1, b.x2, b.z2, b.y, b.h - current_feature.ceiling);
    }

    clearBuildingSpace();

    if (last_bounds_hash != current_bounds_hash)
    {
      last_bounds_hash = current_bounds_hash;
      resetBorders();
    }

    if (current_feature != null)
    {
      switch (current_feature.name)
      {
      case "Bars":
        final var bsize = 4;
        int c = ThreadLocalRandom.current().nextInt(2, 5);

        while (c-- != 0)
        {
          var height = b.y + ThreadLocalRandom.current().nextInt(6, b.h - 1 - bsize);
          var slide = ThreadLocalRandom.current().nextInt(b.getWidth() / 2 - bsize) + 1;

          ZvH.editSession.setBlocks((Region) new CuboidRegion(BlockVector3.at(b.x2 - slide, height, b.z2 - 1),
              BlockVector3.at(b.x2 - slide - bsize, height + bsize, b.z1 + 1)), bars_p);
          ZvH.editSession.setBlocks((Region) new CuboidRegion(BlockVector3.at(b.x1 + slide, height, b.z2 - 1),
              BlockVector3.at(b.x1 + slide + bsize, height + bsize, b.z1 + 1)), bars_p);
        }
        break;

      case "Pillars":
        final var psize = (2) - 1;
        int c1 = ThreadLocalRandom.current().nextInt(2, 10);

        while (c1-- != 0)
        {
          var height = ThreadLocalRandom.current().nextInt(4, b.h - 8);
          var x_slide = ThreadLocalRandom.current().nextInt(b.getWidth() / 2 - psize);
          var z_slide = ThreadLocalRandom.current().nextInt(b.getLength() - psize - 2);

          ZvH.editSession
              .setBlocks(
                  (Region) new CuboidRegion(BlockVector3.at(b.x2 - x_slide - 1, b.y + 1, b.z1 + z_slide + 1),
                      BlockVector3.at(b.x2 - x_slide - psize - 1, b.y + height, b.z1 + z_slide + psize + 1)),
                  pillars_p);
          ZvH.editSession
              .setBlocks(
                  (Region) new CuboidRegion(BlockVector3.at(b.x1 + x_slide + 1, b.y + 1, b.z1 + z_slide + 1),
                      BlockVector3.at(b.x1 + x_slide + psize + 1, b.y + height, b.z1 + z_slide + psize + 1)),
                  pillars_p);
        }
        resetSpawns();
        break;

      case "Fortress":
        ClipboardFormat format = ClipboardFormats.findByFile(fortress_schem);
        try (ClipboardReader reader = format.getReader(new FileInputStream(fortress_schem)))
        {
          Clipboard clipboard = reader.read();
          clipboard.replaceBlocks(clipboard.getRegion(), red_filter, fortress_p);
          clipboard.paste(ZvH.editSession,
              BlockVector3.at(b.getCenter().getBlockX() + 2, b.y + 1, b.getCenter().getBlockZ()), false, false, false);
          // Operations.complete(clipboard.commit());
          clipboard.close();
        } catch (IOException e)
        {
          Logger.Get().log(Level.SEVERE, "Failed to load schematic");
          e.printStackTrace();
        }
        break;

      case "Bridge":
        ClipboardFormat format1 = ClipboardFormats.findByFile(bridge_schem);
        try (ClipboardReader reader = format1.getReader(new FileInputStream(bridge_schem)))
        {
          Clipboard clipboard = reader.read();

          final var origin = BlockVector3.at(current_size.humanSpawn.getX() + 20, b.y + 1, b.z2 - 5);
          clipboard.paste(ZvH.editSession, origin, false, false, false);
          final var cbh = new ClipboardHolder(clipboard);
          final var flipZ = new AffineTransform()
              .scale(BlockVector3.UNIT_Z.abs().multiply(-2).add(1, 1, 1).toVector3());
          cbh.setTransform(flipZ);
          var op = cbh.createPaste(ZvH.editSession)
              .to(BlockVector3.at(current_size.humanSpawn.getX() + 20, b.y + 1, b.z1 + 5)).build();
          Operations.complete(op);
          cbh.close();
          ZvH.editSession.flushQueue();

          // relative to schematic
          var seg_reg = new CuboidRegion(BlockVector3.at(-2, 25, -11), BlockVector3.at(2, 23, -16));
          // relative to world
          seg_reg.shift(origin);

          var copy = new ForwardExtentCopy(ZvH.editSession, seg_reg, ZvH.editSession, seg_reg.getMinimumPoint());
          copy.setRepetitions((b.getLength() - 36) / 6);
          copy.setTransform(
              new AffineTransform().translate(BlockVector3.UNIT_MINUS_Z.multiply(seg_reg.getDimensions())));
          Operations.complete(copy);
          ZvH.editSession.flushQueue();

          // randomize blocks
          var reg = new CuboidRegion(BlockVector3.at(current_size.humanSpawn.getX() + 20 + 2, b.y + 1, b.z2 - 5),
              BlockVector3.at(current_size.humanSpawn.getX() + 20 - 2, b.y + 1 + 26, b.z1 + 5));
          ZvH.editSession.replaceBlocks(reg, red_filter, bridge_p1);
          ZvH.editSession.replaceBlocks(reg, orange_filter, bridge_p2);
        } catch (IOException e)
        {
          Logger.Get().log(Level.SEVERE, "Failed to load schematic");
          e.printStackTrace();
        }
        break;

      case "Grid":
        final int size = 9;
        final var cb = new CuboidRegion(BlockVector3.at(b.x1 + 1, b.y + b.h - 18 - 1, b.z1 + 1),
            BlockVector3.at(b.x2 - 1, b.y + b.h - 16 - 1, b.z2 - 1));
        cb.forEach(block -> {
          if ((block.getX() % size == 0) || (block.getZ() % size == 0))
          {
            ZvH.editSession.setBlock(block, grid_p);
          }
        });
        break;

      case "Scatter":
        ZvH.editSession.setBlocks((Region) new CuboidRegion(BlockVector3.at(b.x1 + 1, b.y + 1, b.z1 + 1),
            BlockVector3.at(b.x2 - 1, b.y + b.h - 1, b.z2 - 1)), scatter_p);
        resetSpawns();
        break;
      }
    }
    ZvH.editSession.flushQueue();

    for (var e : ZvH.world.getEntitiesByClasses(Item.class, Arrow.class, TNTPrimed.class, ThrownPotion.class,
        FallingBlock.class))
    {
      e.remove();
    }
  }

  // draws a circle, in a block game!
  static void drawCircle(int x, int y, int z, int radius, Material material, int precision)
  {

    double angle = 0;

    // Loop through points around the circle
    for (int i = 0; i < precision; i++)
    {

      // update angle
      angle += 2 * Math.PI / precision;

      // get coords of nearest block
      int dX = x + (int) Math.round(radius * Math.cos(angle));
      int dZ = z + (int) Math.round(radius * Math.sin(angle));

      ZvH.world.setBlockData(dX, y, dZ, material.createBlockData());
    }
  }

  // generate a zombie safe zone in the map
  static void genZSafe(Vector pos, int radius)
  {
    drawCircle(pos.getBlockX(), pos.getBlockY() - 1, pos.getBlockZ(), radius, Material.LIGHT_GRAY_CONCRETE_POWDER,
        radius * radius * 3);
  }

  static int getBoundsHash(MapSize size, MapFeature feature)
  {
    // good enough
    return size.bounds.hashCode() + (feature == null ? 0 : feature.ceiling);
  }
}