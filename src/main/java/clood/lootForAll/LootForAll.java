package clood.lootForAll;

import clood.lootForAll.database.DatabaseManager;
import clood.lootForAll.listeners.BlockListener;
import clood.lootForAll.listeners.ChestListener;
import clood.lootForAll.listeners.PlayerListener;
import clood.lootForAll.storage.InventoryStorage;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class LootForAll extends JavaPlugin {

    private DatabaseManager databaseManager;
    private InventoryStorage inventoryStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        try {
            // Initialize database
            databaseManager = new DatabaseManager(getConfig(), getDataFolder(), getLogger());

            // Initialize storage
            inventoryStorage = new InventoryStorage(databaseManager, getLogger());
            inventoryStorage.loadAll();

            // Register listeners
            getServer().getPluginManager().registerEvents(new ChestListener(inventoryStorage, this, getLogger()), this);
            getServer().getPluginManager().registerEvents(new BlockListener(inventoryStorage, getLogger()), this);
            getServer().getPluginManager().registerEvents(new PlayerListener(inventoryStorage, getLogger()), this);

            getLogger().info("LootForAll has been enabled with " + databaseManager.getDbType() + " database support!");
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (inventoryStorage != null) {
                inventoryStorage.saveAll();
                int size = inventoryStorage.getPerPlayerChests().size();
                inventoryStorage.getPerPlayerChests().clear();
                getLogger().info("LootForAll disabled, saved and cleared " + size + " per-player inventories");
            }

            if (databaseManager != null && !databaseManager.isClosed()) {
                databaseManager.close();
            }
        } catch (SQLException e) {
            getLogger().severe("Error closing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
