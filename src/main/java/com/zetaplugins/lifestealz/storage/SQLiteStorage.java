package com.zetaplugins.lifestealz.storage;

import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.connectionPool.ConnectionPool;
import com.zetaplugins.lifestealz.storage.connectionPool.SQLiteConnectionPool;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class SQLiteStorage extends SQLStorage {
    private final SQLiteConnectionPool connectionPool;

    public SQLiteStorage(LifeStealZ plugin) {
        super(plugin);
        connectionPool = new SQLiteConnectionPool(getPlugin().getDataFolder().getPath() + "/userData.db");
    }

    @Override
    protected void migrateDatabase() {
        try (
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA table_info(hearts)")
        ) {
            boolean hasFirstJoin = false;
            boolean hasGraceOffset = false;

            while (resultSet.next()) {
                if ("firstJoin".equalsIgnoreCase(resultSet.getString("name"))) {
                    hasFirstJoin = true;
                    continue;
                }

                if ("graceOffset".equalsIgnoreCase(resultSet.getString("name"))) {
                    hasGraceOffset = true;
                    continue;
                }
            }

            if (hasFirstJoin) {
                getPlugin().getLogger().info("Removing 'firstJoin' column from 'hearts' table.");
                statement.executeUpdate("ALTER TABLE hearts DROP COLUMN firstJoin");
            }

            if (!hasGraceOffset) {
                getPlugin().getLogger().info("Adding 'graceOffset' column to 'hearts' table.");
                statement.executeUpdate("ALTER TABLE hearts ADD COLUMN graceOffset BIGINT DEFAULT 0");
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to migrate database: ", e);
        }
    }

    @Override
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    @Override
    protected String getInserOrReplaceStatement() {
        return "INSERT OR REPLACE INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers, graceOffset) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }
}
