package clood.lootForAll.storage;

import clood.lootForAll.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class InventoryStorage {

  private final Map<String, Inventory> perPlayerChests = new ConcurrentHashMap<>();
  private final DatabaseManager databaseManager;
  private final Logger logger;

  public InventoryStorage(DatabaseManager databaseManager, Logger logger) {
    this.databaseManager = databaseManager;
    this.logger = logger;
  }

  public void loadAll() throws SQLException {
    String query = "SELECT location_key, player_uuid, inventory_size, inventory_data FROM player_chests";

    try (Connection conn = databaseManager.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(query)) {

      int loaded = 0;
      while (rs.next()) {
        String locationKey = rs.getString("location_key");
        String playerUuid = rs.getString("player_uuid");
        int size = rs.getInt("inventory_size");
        byte[] data = rs.getBytes("inventory_data");

        try {
          ItemStack[] contents = InventorySerializer.deserialize(data);
          Inventory inv = Bukkit.createInventory(null, size, Component.text("Chest"));
          inv.setContents(contents);

          String key = locationKey + ":" + playerUuid;
          perPlayerChests.put(key, inv);
          loaded++;
        } catch (IOException | ClassNotFoundException e) {
          logger.warning("Failed to deserialize inventory for " + locationKey + ":" + playerUuid);
          e.printStackTrace();
        }
      }

      logger.info("Loaded " + loaded + " per-player inventories from database");
    }
  }

  public void saveAll() throws SQLException {
    String upsert = getUpsertQuery();

    try (Connection conn = databaseManager.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(upsert)) {
      int saved = 0;
      for (Map.Entry<String, Inventory> entry : perPlayerChests.entrySet()) {
        String[] parts = entry.getKey().split(":");
        if (parts.length != 5)
          continue;

        String locationKey = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
        String playerUuid = parts[4];
        Inventory inv = entry.getValue();

        try {
          byte[] data = InventorySerializer.serialize(inv.getContents());
          pstmt.setString(1, locationKey);
          pstmt.setString(2, playerUuid);
          pstmt.setInt(3, inv.getSize());
          pstmt.setBytes(4, data);
          pstmt.addBatch();
          saved++;
        } catch (IOException e) {
          logger.warning("Failed to serialize inventory for " + entry.getKey());
          e.printStackTrace();
        }
      }

      pstmt.executeBatch();
      logger.info("Saved " + saved + " per-player inventories to database");
    }
  }

  public void saveAsync(String key, Inventory inventory) {
    String upsert = getUpsertQuery();
    String[] parts = key.split(":");
    if (parts.length != 5)
      return;

    String locationKey = parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3];
    String playerUuid = parts[4];

    Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("LootForAll"), () -> {
      try (Connection conn = databaseManager.getConnection();
          PreparedStatement pstmt = conn.prepareStatement(upsert)) {
        byte[] data = InventorySerializer.serialize(inventory.getContents());
        pstmt.setString(1, locationKey);
        pstmt.setString(2, playerUuid);
        pstmt.setInt(3, inventory.getSize());
        pstmt.setBytes(4, data);
        pstmt.executeUpdate();
      } catch (SQLException | IOException e) {
        logger.warning("Failed to save inventory for " + key);
        e.printStackTrace();
      }
    });
  }

  public void deletePlayerInventoriesAsync(String playerUuid) {
    Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("LootForAll"), () -> {
      String delete = "DELETE FROM player_chests WHERE player_uuid = ?";
      try (Connection conn = databaseManager.getConnection();
          PreparedStatement pstmt = conn.prepareStatement(delete)) {
        pstmt.setString(1, playerUuid);
        int deleted = pstmt.executeUpdate();
        logger.info("Deleted " + deleted + " inventories for player " + playerUuid + " from database");
      } catch (SQLException e) {
        logger.warning("Failed to delete inventories for player " + playerUuid);
        e.printStackTrace();
      }
    });
  }

  private String getUpsertQuery() {
    if (databaseManager.getDbType().equals("sqlite")) {
      return "INSERT INTO player_chests (location_key, player_uuid, inventory_size, inventory_data, last_updated) " +
          "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP) " +
          "ON CONFLICT(location_key, player_uuid) DO UPDATE SET " +
          "inventory_size = excluded.inventory_size, " +
          "inventory_data = excluded.inventory_data, " +
          "last_updated = excluded.last_updated";
    } else {
      return "INSERT INTO player_chests (location_key, player_uuid, inventory_size, inventory_data) " +
          "VALUES (?, ?, ?, ?) " +
          "ON DUPLICATE KEY UPDATE " +
          "inventory_size = VALUES(inventory_size), " +
          "inventory_data = VALUES(inventory_data)";
    }
  }

  public Map<String, Inventory> getPerPlayerChests() {
    return perPlayerChests;
  }
}
