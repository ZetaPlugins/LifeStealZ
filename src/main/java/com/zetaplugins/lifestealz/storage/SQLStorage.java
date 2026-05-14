package com.zetaplugins.lifestealz.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.zetaplugins.lifestealz.LifeStealZ;
import com.zetaplugins.lifestealz.storage.connectionPool.ConnectionPool;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public abstract class SQLStorage extends Storage {
    private static final String CSV_SEPARATOR = ",";
    private final ConcurrentMap<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    public SQLStorage(LifeStealZ plugin) {
        super(plugin);
    }

    @Override
    public void init() {
        try (Connection connection = getConnection()) {
            if (connection == null) return;
            try (Statement statement = connection.createStatement()) {
                StringBuilder sql = new StringBuilder();
                sql.append("CREATE TABLE IF NOT EXISTS hearts (")
                        .append("uuid CHAR(36) PRIMARY KEY, ")
                        .append("name VARCHAR(64) NOT NULL, ")
                        .append("maxhp FLOAT NOT NULL DEFAULT 20.0, ")
                        .append("hasbeenRevived SMALLINT NOT NULL DEFAULT 0, ")
                        .append("craftedHearts SMALLINT UNSIGNED NOT NULL DEFAULT 0, ")
                        .append("craftedRevives SMALLINT UNSIGNED NOT NULL DEFAULT 0, ")
                        .append("killedOtherPlayers MEDIUMINT UNSIGNED NOT NULL DEFAULT 0, ")
                        .append("firstJoin BIGINT UNSIGNED NOT NULL")
                        .append(");");
                statement.executeUpdate(sql.toString());

                migrateDatabase();
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to initialize SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to initialize SQL database:", e);
        }
    }

    public abstract ConnectionPool getConnectionPool();

    public Connection getConnection() throws SQLException {
        return getConnectionPool().getConnection();
    }

    @Override
    public PlayerData load(UUID uuid) {
        PlayerData cachedPlayerData = playerDataCache.get(uuid);
        if (cachedPlayerData != null) {
            return cachedPlayerData;
        }

        final String sql = "SELECT * FROM hearts WHERE uuid = ?";

        try (Connection connection = getConnection()) {
            if (connection == null) return null;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, uuid.toString());
                statement.setQueryTimeout(30);

                try (ResultSet resultSet = statement.executeQuery()) {

                    if (!resultSet.next()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player == null) return null;
                        PlayerData newPlayerData = new PlayerData(player.getName(), uuid);
                        save(newPlayerData);
                        cachePlayerData(newPlayerData);
                        return newPlayerData;
                    }

                    PlayerData playerData = mapResultSetToPlayerData(resultSet, uuid);
                    cachePlayerData(playerData);
                    return playerData;
                } catch (SQLException e) {
                    getPlugin().getLogger().log(Level.SEVERE, "Failed to load player data from SQL database:", e);
                    return null;
                }
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to load player data from SQL database:", e);
                return null;
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to load player data from SQL database:", e);
            return null;
        }
    }

    private PlayerData mapResultSetToPlayerData(ResultSet resultSet, UUID uuid) throws SQLException {
        PlayerData playerData = new PlayerData(resultSet.getString("name"), uuid);
        playerData.setMaxHealth(resultSet.getDouble("maxhp"));
        playerData.setHasBeenRevived(resultSet.getInt("hasbeenRevived"));
        playerData.setCraftedHearts(resultSet.getInt("craftedHearts"));
        playerData.setCraftedRevives(resultSet.getInt("craftedRevives"));
        playerData.setKilledOtherPlayers(resultSet.getInt("killedOtherPlayers"));
        playerData.setFirstJoin(resultSet.getLong("firstJoin"));
        playerData.clearModifiedFields();
        return playerData;
    }

    @Override
    public void save(PlayerData playerData) {
        // This uses standard SQL syntax to work with all SQL databases, but may not be optimal for all (e.g. H2 or MySQL)
        if (!playerData.hasChanges()) {
            cachePlayerData(playerData);
            return;
        }

        try (Connection connection = getConnection()) {
            if (connection == null) return;

            boolean exists = checkIfEntryExists(connection, playerData.getUuid());

            boolean saved;
            if (exists) {
                saved = updatePlayerData(connection, playerData);
            } else {
                saved = insertPlayerData(connection, playerData);
            }

            if (saved) {
                cachePlayerData(playerData);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to save player data:", e);
        }
    }

    protected void cachePlayerData(PlayerData playerData) {
        if (playerData == null) return;

        try {
            playerDataCache.put(UUID.fromString(playerData.getUuid()), playerData);
        } catch (IllegalArgumentException e) {
            getPlugin().getLogger().log(Level.WARNING, "Failed to cache player data with invalid UUID: " + playerData.getUuid(), e);
        }
    }

    protected void clearPlayerDataCache() {
        playerDataCache.clear();
    }

    /**
     * Check if a player entry exists in the database
     * @param connection Connection to the database
     * @param uuid UUID of the player to check
     * @return True if the player entry exists, false otherwise
     */
    private boolean checkIfEntryExists(Connection connection, String uuid) {
        final String selectQuery = "SELECT 1 FROM hearts WHERE uuid = ?";

        try (PreparedStatement selectStmt = connection.prepareStatement(selectQuery);) {
            selectStmt.setString(1, uuid);
            try (ResultSet resultSet = selectStmt.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to check if player entry exists:", e);
            return false;
        }
    }

    /**
     * Insert player data into the database
     * @param connection Connection to the database
     * @param playerData Player data to insert
     * @return True if the insert was successful, false otherwise
     */
    private boolean insertPlayerData(Connection connection, PlayerData playerData) {
        final String insertQuery = "INSERT INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers, firstJoin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
            insertStmt.setString(1, playerData.getUuid());
            insertStmt.setString(2, playerData.getName());
            insertStmt.setDouble(3, playerData.getMaxHealth());
            insertStmt.setInt(4, playerData.getHasBeenRevived());
            insertStmt.setInt(5, playerData.getCraftedHearts());
            insertStmt.setInt(6, playerData.getCraftedRevives());
            insertStmt.setInt(7, playerData.getKilledOtherPlayers());
            insertStmt.setLong(8, playerData.getFirstJoin());
            insertStmt.executeUpdate();

            playerData.clearModifiedFields();

            return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to insert player data:", e);
            return false;
        }
    }

    /**
     * Update player data in the database
     * @param connection Connection to the database
     * @param playerData Player data to update
     * @return True if the update was successful, false otherwise
     */
    private boolean updatePlayerData(Connection connection, PlayerData playerData) {
        StringBuilder updateQuery = new StringBuilder("UPDATE hearts SET ");
        List<Object> params = new ArrayList<>();

        for (String field : playerData.getModifiedFields()) {
            updateQuery.append(field).append(" = ?, ");
            switch (field) {
                case "maxhp":
                    params.add(playerData.getMaxHealth());
                    break;
                case "hasbeenRevived":
                    params.add(playerData.getHasBeenRevived());
                    break;
                case "craftedHearts":
                    params.add(playerData.getCraftedHearts());
                    break;
                case "craftedRevives":
                    params.add(playerData.getCraftedRevives());
                    break;
                case "killedOtherPlayers":
                    params.add(playerData.getKilledOtherPlayers());
                    break;
                case "firstJoin":
                    params.add(playerData.getFirstJoin());
                    break;
            }
        }

        updateQuery.setLength(updateQuery.length() - 2); // Remove the last comma and space
        updateQuery.append(" WHERE uuid = ?");
        params.add(playerData.getUuid());

        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery.toString())) {
            for (int i = 0; i < params.size(); i++) {
                updateStmt.setObject(i + 1, params.get(i));
            }
            updateStmt.executeUpdate();

            playerData.clearModifiedFields();
            return true;
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to update player data:", e);
            return false;
        }
    }

    @Override
    public PlayerData load(String uuid) {
        return load(UUID.fromString(uuid));
    }

    @Override
    public List<UUID> getEliminatedPlayers() {
        List<UUID> eliminatedPlayers = new ArrayList<>();

        int minHearts = getPlugin().getConfig().getInt("minHearts");

        try (Connection connection = getConnection()) {
            if (connection == null) return eliminatedPlayers;

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                ResultSet resultSet = statement.executeQuery("SELECT uuid FROM hearts WHERE maxhp <= " + minHearts * 2 + ".0");

                while (resultSet.next()) {
                    eliminatedPlayers.add(UUID.fromString(resultSet.getString("uuid")));
                }
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to get eliminated players from SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to get eliminated players from SQL database:", e);
        }

        return eliminatedPlayers;
    }


    @Override
    public String export(String fileName) {
        String filePath = getPlugin().getDataFolder().getPath() + "/" + fileName + ".csv";
        try (Connection connection = getConnection()) {
            if (connection == null) return null;

            try (Statement statement = connection.createStatement()) {
                ResultSet resultSet = statement.executeQuery("SELECT * FROM hearts");

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                    while (resultSet.next()) {
                        String line = resultSet.getString("uuid") + CSV_SEPARATOR +
                                resultSet.getString("name") + CSV_SEPARATOR +
                                resultSet.getDouble("maxhp") + CSV_SEPARATOR +
                                resultSet.getInt("hasbeenRevived") + CSV_SEPARATOR +
                                resultSet.getInt("craftedHearts") + CSV_SEPARATOR +
                                resultSet.getInt("craftedRevives") + CSV_SEPARATOR +
                                resultSet.getInt("killedOtherPlayers") + CSV_SEPARATOR +
                                resultSet.getLong("firstJoin");
                        writer.write(line);
                        writer.newLine();
                    }
                }
            } catch (SQLException | IOException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to export player data to CSV file:", e);
                return null;
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to export player data to CSV file:", e);
            return null;
        }
        return filePath;
    }

    protected abstract String getInserOrReplaceStatement();

    @Override
    public void importData(String fileName) {
        String filePath = getPlugin().getDataFolder().getPath() + "/" + fileName;
        long startTime = System.currentTimeMillis();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
             Connection connection = getConnection()) {

            if (connection == null) return;

            // We are disabling auto-commit to insert all data in a single transaction to improve performance
            connection.setAutoCommit(false);

            String sql = getInserOrReplaceStatement();// use db specific insert or replace statement
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                String line;
                int batchSize = 0;
                int totalRows = 0;
                while ((line = reader.readLine()) != null) {
                    String[] data = line.split(CSV_SEPARATOR);

                    if (data.length != 8) {
                        getPlugin().getLogger().severe("Invalid CSV format. Expected 8 columns, but got " + data.length);
                        continue;
                    }

                    totalRows++;

                    statement.setString(1, data[0]);  // uuid
                    statement.setString(2, data[1]);  // name
                    statement.setDouble(3, Double.parseDouble(data[2])); // maxhp
                    statement.setInt(4, Integer.parseInt(data[3])); // hasbeenRevived
                    statement.setInt(5, Integer.parseInt(data[4])); // craftedHearts
                    statement.setInt(6, Integer.parseInt(data[5])); // craftedRevives
                    statement.setInt(7, Integer.parseInt(data[6])); // killedOtherPlayers
                    statement.setLong(8, Long.parseLong(data[7])); // firstJoin

                    statement.addBatch();
                    batchSize++;

                    // Execute batch every 500 inserts
                    if (batchSize % 500 == 0) {
                        getPlugin().getLogger().info("Imported " + totalRows + " player data entries. Committing batch...");
                        statement.executeBatch();
                    }
                }
                statement.executeBatch();

                connection.commit();
                clearPlayerDataCache();

                long endTime = System.currentTimeMillis();
                getPlugin().getLogger().info("Imported " + totalRows + " player data entries in " + (endTime - startTime) + "ms");

            } catch (SQLException e) {
                connection.rollback();
                getPlugin().getLogger().log(Level.SEVERE, "Failed to import player data:", e);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (IOException | SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to read CSV file:", e);
        }
    }

    @Override
    public int reviveAllPlayers(int minHearts, int reviveHearts, int maxRevives, boolean bypassReviveLimit) {
        int affectedPlayers = 0;

        String sql = "UPDATE hearts SET maxhp = ?, hasbeenRevived = hasbeenRevived + 1 WHERE maxhp <= ? AND (hasbeenRevived < ?)";

        if (bypassReviveLimit || maxRevives < 0) {
            sql = "UPDATE hearts SET maxhp = ?, hasbeenRevived = hasbeenRevived + 1 WHERE maxhp <= ?";
        }

        try (Connection connection = getConnection()) {
            if (connection == null) return affectedPlayers;

            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, reviveHearts * 2);
                pstmt.setDouble(2, minHearts * 2);

                if (!bypassReviveLimit && maxRevives >= 0) {
                    pstmt.setInt(3, maxRevives);
                }

                affectedPlayers = pstmt.executeUpdate();
                if (affectedPlayers > 0) {
                    clearPlayerDataCache();
                }
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to revive all players in SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to revive all players in SQL database:", e);
        }

        return affectedPlayers;
    }

    @Override
    public List<String> getPlayerNames() {
        List<String> playerNames = new ArrayList<>();

        try (Connection connection = getConnection()) {
            if (connection == null) return playerNames;

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                ResultSet resultSet = statement.executeQuery("SELECT name FROM hearts");

                while (resultSet.next()) {
                    playerNames.add(resultSet.getString("name"));
                }
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to load player names from SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to load player names from SQL database:", e);
        }

        return playerNames;
    }

    @Override
    public List<String> getEliminatedPlayerNames() {
        List<String> eliminatedPlayerNames = new ArrayList<>();

        try (Connection connection = getConnection()) {
            if (connection == null) return eliminatedPlayerNames;

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30);

                ResultSet resultSet = statement.executeQuery("SELECT name FROM hearts WHERE maxhp <= 0.0");

                while (resultSet.next()) {
                    eliminatedPlayerNames.add(resultSet.getString("name"));
                }
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to get eliminated players from SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to get eliminated players from SQL database:", e);
        }

        return eliminatedPlayerNames;
    }

    @Override
    public void clearDatabase() {
        try (Connection connection = getConnection()) {
            if (connection == null) return;

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM hearts");
                clearPlayerDataCache();
            } catch (SQLException e) {
                getPlugin().getLogger().log(Level.SEVERE, "Failed to clear SQL database:", e);
            }
        } catch (SQLException e) {
            getPlugin().getLogger().log(Level.SEVERE, "Failed to clear SQL database:", e);
        }
    }
}
