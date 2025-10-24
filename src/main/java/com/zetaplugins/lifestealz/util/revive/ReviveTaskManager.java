package com.zetaplugins.lifestealz.util.revive;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages beacon revive tasks
 */
public class ReviveTaskManager {
    private final Map<Location, ReviveTask> reviveTasks;

    public ReviveTaskManager() {
        this.reviveTasks = new HashMap<>();
    }

    /**
     * Get the revive task for the specified beacon location.
     * @param location the location of the revive beacon
     * @return the revive task, or null if none exists
     */
    public ReviveTask getReviveTask(Location location) {
        return this.reviveTasks.get(location);
    }

    /**
     * Adds a revive task for the specified beacon location.
     * @param location the location of the revive beacon
     * @param reviveTask the revive task to add
     */
    public void addReviveTask(Location location, ReviveTask reviveTask) {
        this.reviveTasks.put(location, reviveTask);
    }

    /**
     * Checks if there is an active revive task at the specified beacon location.
     * @param location the location to check
     * @return true if there is a revive task at the location, false otherwise
     */
    public boolean isReviving(Location location) {
        return this.reviveTasks.containsKey(location);
    }

    /**
     * Removes the revive task associated with the specified location.
     * @param location the location of the revive task to remove
     */
    public void removeReviveTask(Location location) {
        this.reviveTasks.remove(location);
    }

    /**
     * Checks if any revive task is targeting the specified player UUID.
     * @param playerId the UUID of the player to check
     * @return true if the player is being revived, false otherwise
     */
    public boolean isRevivingTarget(UUID playerId) {
        return reviveTasks.values()
                .stream()
                .anyMatch(task -> task.target().equals(playerId));
    }
}
