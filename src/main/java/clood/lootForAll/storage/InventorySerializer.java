package clood.lootForAll.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InventorySerializer {

  public static byte[] serialize(ItemStack[] items) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
    dataOutput.writeInt(items.length);
    for (ItemStack item : items) {
      dataOutput.writeObject(item);
    }
    dataOutput.close();
    return outputStream.toByteArray();
  }

  public static ItemStack[] deserialize(byte[] data) throws IOException, ClassNotFoundException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
    BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
    int length = dataInput.readInt();
    ItemStack[] items = new ItemStack[length];
    for (int i = 0; i < length; i++) {
      items[i] = (ItemStack) dataInput.readObject();
    }
    dataInput.close();
    return items;
  }
}
