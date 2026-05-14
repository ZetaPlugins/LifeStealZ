package com.zetaplugins.lifestealz.storage;

import com.zetaplugins.lifestealz.LifeStealZ;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract class for Storage classes that share the same MySQL syntax.
 */
public abstract class MySQLSyntaxStorage extends SQLStorage {
    public MySQLSyntaxStorage(LifeStealZ plugin) {
        super(plugin);
    }

    @Override
    public void save(PlayerData playerData) {
        String insertOrUpdateQuery = "INSERT INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers, firstJoin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), " +
                "maxhp = VALUES(maxhp), " +
                "hasbeenRevived = VALUES(hasbeenRevived), " +
                "craftedHearts = VALUES(craftedHearts), " +
                "craftedRevives = VALUES(craftedRevives), " +
                "killedOtherPlayers = VALUES(killedOtherPlayers), " +
                "firstJoin = VALUES(firstJoin)";

        try (Connection connection = getConnection()) {
            if (connection == null) return;

            try (PreparedStatement stmt = connection.prepareStatement(insertOrUpdateQuery)) {

                stmt.setString(1, playerData.getUuid());
                stmt.setString(2, playerData.getName());
                stmt.setDouble(3, playerData.getMaxHealth());
                stmt.setInt(4, playerData.getHasBeenRevived());
                stmt.setInt(5, playerData.getCraftedHearts());
                stmt.setInt(6, playerData.getCraftedRevives());
                stmt.setInt(7, playerData.getKilledOtherPlayers());
                stmt.setLong(8, playerData.getFirstJoin());

                stmt.executeUpdate();

                playerData.clearModifiedFields();
                cachePlayerData(playerData);
            } catch (SQLException e) {
                getPlugin().getLogger().severe("Failed to save player data to database: " + e.getMessage());
            }
        } catch (SQLException e) {
            getPlugin().getLogger().severe("Failed to save player data to database: " + e.getMessage());
        }
    }

    @Override
    protected String getInserOrReplaceStatement() {
        return "INSERT INTO hearts (uuid, name, maxhp, hasbeenRevived, craftedHearts, craftedRevives, killedOtherPlayers, firstJoin) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), " +
                "maxhp = VALUES(maxhp), " +
                "hasbeenRevived = VALUES(hasbeenRevived), " +
                "craftedHearts = VALUES(craftedHearts), " +
                "craftedRevives = VALUES(craftedRevives), " +
                "killedOtherPlayers = VALUES(killedOtherPlayers), " +
                "firstJoin = VALUES(firstJoin)";
    }
}
