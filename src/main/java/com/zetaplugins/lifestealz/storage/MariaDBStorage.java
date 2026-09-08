package com.zetaplugins.lifestealz.storage;

import org.bukkit.configuration.file.FileConfiguration;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.connectionPool.ConnectionPool;
import com.zetaplugins.lifestealz.storage.connectionPool.MariaDBConnectionPool;

import java.sql.*;
import java.util.logging.Level;

/**
 * Storage class for MariaDB.
 */
public final class MariaDBStorage extends MySQLSyntaxStorage {
    private final MariaDBConnectionPool connectionPool;

    public MariaDBStorage(LifeStealZ plugin) {
        super(plugin);

        FileConfiguration config = getPlugin().getConfigManager().getStorageConfig();

        final String HOST = config.getString("host");
        final String PORT = config.getString("port");
        final String DATABASE = config.getString("database");
        final String USERNAME = config.getString("username");
        final String PASSWORD = config.getString("password");

        connectionPool = new MariaDBConnectionPool(HOST, PORT, DATABASE, USERNAME, PASSWORD);
    }

    @Override
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    @Override
    protected void migrateDatabase() {
        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                                + " WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND TABLE_NAME = 'hearts'"
                                + " AND COLUMN_NAME = 'firstJoin'"
                )
        ) {
            if (resultSet.next()) {
                getPlugin().getLogger().info("Removing 'firstJoin' column from 'hearts' table.");
                statement.executeUpdate("ALTER TABLE hearts DROP COLUMN firstJoin;");
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to migrate database: ", e);
        }

        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                                + " WHERE TABLE_SCHEMA = DATABASE()"
                                + " AND TABLE_NAME = 'hearts'"
                                + " AND COLUMN_NAME = 'graceOffset'"
                )
        ) {
            if (!resultSet.next()) {
                getPlugin().getLogger().info("Adding 'graceOffset' column to 'hearts' table.");
                statement.executeUpdate("ALTER TABLE hearts ADD COLUMN graceOffset BIGINT DEFAULT 0");
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to migrate database: ", e);
        }
    }
}
