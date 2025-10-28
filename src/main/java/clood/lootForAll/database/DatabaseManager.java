package clood.lootForAll.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {

  private final HikariDataSource dataSource;
  private final String dbType;
  private final Logger logger;

  public DatabaseManager(FileConfiguration config, File dataFolder, Logger logger) throws SQLException {
    this.logger = logger;
    this.dbType = config.getString("database.type", "sqlite").toLowerCase();

    HikariConfig hikariConfig = new HikariConfig();

    switch (dbType) {
      case "mysql":
      case "mariadb":
        configureMySQLDatabase(config, hikariConfig);
        break;

      case "sqlite":
      default:
        configureSQLiteDatabase(config, dataFolder, hikariConfig);
        break;
    }

    hikariConfig.setPoolName("LootForAll-Pool");
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(hikariConfig);

    createTables();
  }

  private void configureMySQLDatabase(FileConfiguration config, HikariConfig hikariConfig) {
    String host = config.getString("database.remote.host", "localhost");
    int port = config.getInt("database.remote.port", 3306);
    String database = config.getString("database.remote.database", "lootforall");
    String username = config.getString("database.remote.username", "root");
    String password = config.getString("database.remote.password", "password");
    boolean useSSL = config.getBoolean("database.remote.useSSL", false);
    boolean allowPublicKeyRetrieval = config.getBoolean("database.remote.allowPublicKeyRetrieval", true);

    String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database +
        "?useSSL=" + useSSL + "&allowPublicKeyRetrieval=" + allowPublicKeyRetrieval;

    hikariConfig.setJdbcUrl(jdbcUrl);
    hikariConfig.setUsername(username);
    hikariConfig.setPassword(password);
    hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
    hikariConfig.setMaximumPoolSize(config.getInt("database.remote.maxPoolSize", 10));
    hikariConfig.setMinimumIdle(config.getInt("database.remote.minIdle", 2));
    hikariConfig.setConnectionTimeout(config.getLong("database.remote.connectionTimeout", 30000));

    logger.info("Connecting to MySQL/MariaDB database at " + host + ":" + port + "/" + database);
  }

  private void configureSQLiteDatabase(FileConfiguration config, File dataFolder, HikariConfig hikariConfig) {
    String filename = config.getString("database.sqlite.filename", "lootforall.db");
    String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + "/" + filename;
    dataFolder.mkdirs();

    hikariConfig.setJdbcUrl(url);
    hikariConfig.setDriverClassName("org.sqlite.JDBC");
    hikariConfig.setMaximumPoolSize(1);

    logger.info("Using SQLite database at: " + url);
  }

  private void createTables() throws SQLException {
    String createTable;

    if (dbType.equals("sqlite")) {
      createTable = "CREATE TABLE IF NOT EXISTS player_chests (" +
          "id INTEGER PRIMARY KEY AUTOINCREMENT," +
          "location_key TEXT NOT NULL," +
          "player_uuid TEXT NOT NULL," +
          "inventory_size INTEGER NOT NULL," +
          "inventory_data BLOB NOT NULL," +
          "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
          "UNIQUE(location_key, player_uuid))";
    } else {
      createTable = "CREATE TABLE IF NOT EXISTS player_chests (" +
          "id INT AUTO_INCREMENT PRIMARY KEY," +
          "location_key VARCHAR(255) NOT NULL," +
          "player_uuid VARCHAR(36) NOT NULL," +
          "inventory_size INT NOT NULL," +
          "inventory_data LONGBLOB NOT NULL," +
          "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
          "UNIQUE KEY unique_chest (location_key, player_uuid)," +
          "INDEX idx_location (location_key)," +
          "INDEX idx_player (player_uuid))";
    }

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute(createTable);
    }

    logger.info("Database tables initialized for " + dbType);
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }

  public String getDbType() {
    return dbType;
  }

  public void close() throws SQLException {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }

  public boolean isClosed() {
    return dataSource.isClosed();
  }
}
