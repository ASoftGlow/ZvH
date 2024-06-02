package dev.asoftglow.zvh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

public class SpeciesClassManager
{
  private static final LinkedHashMap<String, ZombieClass> zombie_classes = new LinkedHashMap<>();
  private static final RangeMap<Integer, HumanClass> human_classes = TreeRangeMap.create();
  private static Path classesConfigDirectory;

  public static boolean speciesHasClass(String species, String name)
  {
    if (species.equals("zombie"))
    {
      return zombie_classes.containsKey(name);

    } else if (species.equals("human"))
    {
      return human_classes.get(Integer.valueOf(name)) != null;
    }
    return false;
  }

  public static SpeciesClass speciesGetClass(String species, String name)
  {
    if (species.equals("zombie"))
    {
      return zombie_classes.get(name);

    } else if (species.equals("human"))
    {
      return human_classes.get(Integer.valueOf(name));
    }
    return null;
  }

  public static List<String> speciesGetClassNames(String species)
  {
    if (species.equals("zombie"))
    {
      return new ArrayList<>(zombie_classes.keySet());

    } else if (species.equals("human"))
    {
      var m = human_classes.asMapOfRanges();
      var l = new ArrayList<String>(m.size());
      for (var k : m.keySet())
      {
        l.add(k.lowerEndpoint().toString());
      }
      return l;
    }
    return List.of();
  }

  public static HumanClass getHumanClass(int lvl)
  {
    return human_classes.get(lvl);
  }

  public static void init(Path classesConfigDirectory)
  {
    SpeciesClassManager.classesConfigDirectory = classesConfigDirectory;
    classesConfigDirectory.toFile().mkdir();

    registerZClass("Zombie", Material.ZOMBIE_HEAD, 0);
    registerZClass("Baby_Zombie", Material.CARROT, 4,
        new PotionEffect(PotionEffectType.SPEED, -1, 0, false, false, false),
        new PotionEffect(PotionEffectType.FAST_DIGGING, -1, 0));
    registerZClass("Skeleton", Material.SKELETON_SKULL, 5, new PotionEffect(PotionEffectType.WEAKNESS, -1, 0));
    registerZClass("Slime", Material.SLIME_BALL, 6,
        new PotionEffect(PotionEffectType.JUMP, -1, 2, false, false, false));
    registerZClass("Witch", Material.POTION, 8);
    registerZClass("Spider", Material.STRING, 10);
    registerZClass("Blaze", Material.BLAZE_POWDER, 15);

    registerHClass(Range.closedOpen(0, 10));
    registerHClass(Range.closedOpen(10, 25));
    registerHClass(Range.closedOpen(25, 35));
    registerHClass(Range.closedOpen(35, 50));
    registerHClass(Range.atLeast(50));
  }

  public static void registerZClass(String name, Material icon, int price, PotionEffect... effects)
  {
    zombie_classes.put(name, new ZombieClass(name, icon, price, readClass(name, "zombie"), effects));
    Logger.Get().info("Registered class %s.".formatted(name));
  }

  public static void registerHClass(Range<Integer> lvl_range, PotionEffect... effects)
  {
    human_classes.put(lvl_range, new HumanClass(readClass(lvl_range.lowerEndpoint().toString(), "human"), effects));
  }

  private static ItemStack[] readClass(String name, String species)
  {
    try
    {
      Path fp = classesConfigDirectory.resolve(species).resolve(name);
      if (!Files.exists(fp))
        return null;
      var data = Files.readAllBytes(fp);
      var items = SerializeInventory.deserializeItems(data);
      Logger.Get().info("Loaded class config for %s.".formatted(name));
      return items;

    } catch (IOException e)
    {
      Logger.Get().log(Level.SEVERE, "Failed to read from class config for %s!".formatted(name), e);
      return null;
    }
  }

  public static void writeClass(String name, String species, byte[] data)
  {
    try
    {
      Files.write(classesConfigDirectory.resolve(species).resolve(name), data);
    } catch (IOException e)
    {
      Logger.Get().log(Level.SEVERE, "Failed to save %s class config!".formatted(name), e);
    }
  }

  public static void saveClassFrom(String name, String species, PlayerInventory inventory)
  {
    SpeciesClassManager.writeClass(name, species, SerializeInventory.serialize(inventory));
  }
}
