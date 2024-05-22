package dev.asoftglow.zvh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

public class ZClassManager
{
  public static final LinkedHashMap<String, ZClass> zClasses = new LinkedHashMap<>();
  private static Path classesConfigDirectory;
  private static Logger logger;

  public static void init(Path classesConfigDirectory, Logger logger)
  {
    ZClassManager.classesConfigDirectory = classesConfigDirectory;
    ZClassManager.logger = logger;
    classesConfigDirectory.toFile().mkdir();
  }

  public static void registerZClass(String name, Material icon, int price, PotionEffect... effects)
  {
    zClasses.put(name, new ZClass(name, icon, price, readZClass(name), effects));
    logger.info("Registered class %s.".formatted(name));
  }

  public static ItemStack[] readZClass(String name)
  {
    try
    {
      Path fp = classesConfigDirectory.resolve(name);
      if (!Files.exists(fp))
        return null;
      var data = Files.readAllBytes(fp);
      var items = SerializeInventory.deserializeItems(data);
      logger.info("Loaded class config for %s.".formatted(name));
      return items;

    } catch (IOException e)
    {
      logger.log(Level.SEVERE, "Failed to read from class config for %s!".formatted(name), e);
      return null;
    }
  }

  public static void writeZClass(String name, byte[] data)
  {
    try
    {
      Files.write(classesConfigDirectory.resolve(name), data);
    } catch (IOException e)
    {
      logger.log(Level.SEVERE, "Failed to save %s class config!".formatted(name), e);
    }
  }

  public static void saveZClassFrom(String name, PlayerInventory inventory)
  {
    ZClassManager.writeZClass(name, SerializeInventory.serialize(inventory));
  }
}
