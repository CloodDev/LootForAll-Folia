package clood.lootForAll;

import clood.lootForAll.database.DatabaseManager;
import clood.lootForAll.listeners.BlockListener;
import clood.lootForAll.listeners.ChestListener;
import clood.lootForAll.listeners.PlayerListener;
import clood.lootForAll.storage.InventoryStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;

public final class LootForAll extends JavaPlugin {

    private DatabaseManager databaseManager;
    private InventoryStorage inventoryStorage;
    private File configFile;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        loadCustomConfig();

        try {
            // Initialize database
            databaseManager = new DatabaseManager(config, getDataFolder(), getLogger());

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

    private void loadCustomConfig() {
        // Create config directory
        File configDir = new File(getDataFolder(), "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        // Set config file path
        configFile = new File(configDir, "config.yml");

        // Copy default config if it doesn't exist
        if (!configFile.exists()) {
            try (InputStream in = getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                    getLogger().info("Created default config file at: config/config.yml");
                }
            } catch (IOException e) {
                getLogger().severe("Could not create config file: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Load config
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            loadCustomConfig();
        }
        return config;
    }

    @Override
    public void saveConfig() {
        if (config != null && configFile != null) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                getLogger().severe("Could not save config to " + configFile + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public void reloadConfig() {
        if (configFile != null) {
            config = YamlConfiguration.loadConfiguration(configFile);
        }
    }
}
