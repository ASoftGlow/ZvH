package dev.asoftglow.zvh.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

public final class SerializeInventory
{
  public static byte[] serialize(PlayerInventory playerInventory)
  {
    // This contains contents, armor and offhand (contents are indexes 0 - 35, armor
    // 36 - 39, offhand - 40)
    return serializeItems(playerInventory.getContents());
  }

  public static byte[] serializeItems(ItemStack[] items)
  {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
    {
      DataOutput output = new DataOutputStream(outputStream);
      output.writeInt(items.length);

      for (var item : items)
      {
        if (item == null)
        {
          // Ensure the correct order by including empty/null items
          // Simply remove the write line if you don't want this
          output.writeInt(0);
          continue;
        }

        byte[] bytes = item.serializeAsBytes();
        output.writeInt(bytes.length);
        output.write(bytes);
      }
      return outputStream.toByteArray(); // Base64 encoding is not strictly needed
    } catch (IOException e)
    {
      throw new RuntimeException("Error while writing itemstack", e);
    }
  }

  public static ItemStack[] deserializeItems(byte[] bytes)
  {
    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes))
    {
      DataInputStream input = new DataInputStream(inputStream);
      int count = input.readInt();
      ItemStack[] items = new ItemStack[count];
      for (int i = 0; i < count; i++)
      {
        int length = input.readInt();
        if (length == 0)
        {
          // Empty item, keep entry as null
          continue;
        }

        byte[] itemBytes = new byte[length];
        input.read(itemBytes);
        items[i] = ItemStack.deserializeBytes(itemBytes);
      }
      return items;
    } catch (IOException e)
    {
      throw new RuntimeException("Error while reading itemstack", e);
    }
  }
}