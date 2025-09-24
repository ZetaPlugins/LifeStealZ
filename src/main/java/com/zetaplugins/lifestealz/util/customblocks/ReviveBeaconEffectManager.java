package com.zetaplugins.lifestealz.util.customblocks;

import org.bukkit.Sound;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.zetaplugins.lifestealz.LifeStealZ;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ReviveBeaconEffectManager {
    private final LifeStealZ plugin;
    private final Map<Location, BukkitTask> idleParticleBeacons;
    private final Map<Location, BukkitTask> revivingParticleBeacons;
    private final Map<Location, Set<BlockDisplay>> lasers;
    private final Map<Location, BukkitTask> laserGrowTasks;
    private final Map<Location, BlockDisplay> decoyDisplays;
    private final Map<Location, BukkitTask> bossbarTasks;
    private final Map<Location, BossBar> bossBars;

    public ReviveBeaconEffectManager(LifeStealZ plugin) {
        this.plugin = plugin;
        this.idleParticleBeacons = new HashMap<>();
        this.revivingParticleBeacons = new HashMap<>();
        this.lasers = new HashMap<>();
        this.laserGrowTasks = new HashMap<>();
        this.decoyDisplays = new HashMap<>();
        this.bossbarTasks = new HashMap<>();
        this.bossBars = new HashMap<>();
    }

    /**
     * Starts the idle particle effects for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the particles will be spawned.
     * @param showEnchantParticles Whether to show enchantment particles around the beacon.
     * @param decoyMaterial The material to use for the decoy block display.
     */
    public void startIdleEffects(Location location, boolean showEnchantParticles, Material decoyMaterial) {
        if (idleParticleBeacons.containsKey(getKey(location)) || decoyDisplays.containsKey(getKey(location))) return;

        applyMaterialDecoy(location, decoyMaterial);

        if (!showEnchantParticles) return;

        var runnable = new BukkitRunnable() {
            final Location center = location.clone().add(0.5, 1.0, 0.5);

            public void run() {
                center.getWorld().spawnParticle(Particle.ENCHANT, center, 25, 0.6, 0.5, 0.6, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        idleParticleBeacons.put(getKey(location), runnable);
    }

    /**
     * Starts the reviving particle effects for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the particles will be spawned.
     * @param target The name of the Player who is being revived.
     * @param showLaser Whether to show the laser effect.
     * @param showParticleRing Whether to show the particle ring effect.
     * @param particleColor The color of the particles in the ring.
     * @param innerLaserMaterial The material for the inner laser beam.
     * @param outerLaserMaterial The material for the outer laser beam.
     */
    public void startRevivingEffects(Location location, String target, boolean showLaser, boolean showParticleRing, ParticleColor particleColor, Material innerLaserMaterial, Material outerLaserMaterial, int reviveTime) {
        if (revivingParticleBeacons.containsKey(location) || lasers.containsKey(location)) return;

        if (showLaser) spawnBeaconLaser(location, innerLaserMaterial, outerLaserMaterial);

        location.getWorld().playSound(location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);

        if (showParticleRing) {
            var runnable = new BukkitRunnable() {
                final Location center = location.clone().add(0.5, 1.0, 0.5);

                public void run() {
                    spawnRing(center, particleColor);
                }
            }.runTaskTimer(plugin, 0L, 10L);

            revivingParticleBeacons.put(getKey(location), runnable);
        }

        if (plugin.getConfig().getBoolean("showBossbar")) startBossbarTask(location, target, reviveTime);
    }

    /**
     * Starts a bossbar task for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the bossbar will be shown.
     * @param target The name of the Player who is being revived.
     * @param reviveTime The total time in seconds for the revival process.
     */
    private void startBossbarTask(Location location, String target, int reviveTime) {
        if (bossbarTasks.containsKey(getKey(location))) return;

        int countdown = reviveTime;
        BossBar bossBar = Bukkit.createBossBar(
                "",
                parseBossbarColor(plugin.getConfig().getString("bossbarColor"), BarColor.RED),
                parseBossbarStyle(plugin.getConfig().getString("bossbarStyle"), BarStyle.SOLID)
        );
        bossBar.setVisible(true);

        bossBars.put(getKey(location), bossBar);

        BukkitTask bossbarTask = new BukkitRunnable() {
            int timeleft = countdown;

            public void run() {
                if (timeleft <= 0){
                    bossBar.setVisible(false);
                    bossBar.removeAll();
                    bossBars.remove(getKey(location));
                    this.cancel();
                    return;
                }

                int days = timeleft / 86400;
                int hours = (timeleft % 86400) / 3600;
                int minutes = (timeleft & 3600) / 60;
                int seconds = timeleft % 60;

                String hFormatted = String.format("%02d", hours);
                String mFormatted = String.format("%02d", minutes);
                String sFormatted = String.format("%02d", seconds);

                // Show bossbar to all players
                // (This is in the Task because new players may join during this time)
                for (Player p : Bukkit.getOnlinePlayers()) {
                    bossBar.addPlayer(p);
                }

                bossBar.setProgress((double) timeleft / countdown);

                String title = plugin.getLanguageManager().getString("reviveBossbarTitle")
                        .replace("&target&", target)
                        .replace("&remainingD&", String.valueOf(days))
                        .replace("&remainingH&", hFormatted)
                        .replace("&remainingM&", mFormatted)
                        .replace("&remainingS&", sFormatted)
                        .replace("&locationX&", String.valueOf(location.getBlockX()))
                        .replace("&locationY&", String.valueOf(location.getBlockY()))
                        .replace("&locationZ&", String.valueOf(location.getBlockZ()))
                        .replace("&location&", location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                bossBar.setTitle(ChatColor.translateAlternateColorCodes('&', title));
                timeleft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        bossbarTasks.put(getKey(location), bossbarTask);
    }

    /**
     * Stops the bossbar task for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the bossbar task will be stopped.
     */
    public void stopBossbarTask(Location location) {
        BukkitTask task = bossbarTasks.remove(getKey(location));
        if (task != null) task.cancel();
    }

    /**
     * Removes the bossbar at the specified location.
     * @param location The location of the Revive Beacon where the bossbar will be removed.
     */
    public void removeBossbar(Location location) {
        BossBar bossBar = bossBars.remove(getKey(location));
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
    }

    /**
     * Applies a decoy material at the specified location using block displays. This fakes the appearance of a block
     * @param location The location where the decoy material will be applied.
     * @param decoyMaterial The material to use for the decoy block display.
     */
    private void applyMaterialDecoy(Location location, Material decoyMaterial) {
        if (decoyDisplays.containsKey(getKey(location))) return;

        BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class);
        display.setBlock(decoyMaterial.createBlockData());
        display.setPersistent(true);
        display.setBrightness(new Display.Brightness(8, 8));

        decoyDisplays.put(getKey(location), display);

        animateDecoyGrowth(display);
    }

    /**
     * Animates the growth of a decoy block display at the specified location.
     * @param display The BlockDisplay instance representing the decoy.
     */
    private void animateDecoyGrowth(BlockDisplay display) {
        final float targetSize = 1.01f;
        final float initialHeight = 0f;
        final float width = 1.01f;
        final long tickInterval = 1L;
        final float growSpeed = 0.25f;

        display.setTransformation(new Transformation(
                new Vector3f((1 - width) / 2, 0.5f - (initialHeight / 2f), (1 - width) / 2),
                new Quaternionf(),
                new Vector3f(width, initialHeight, width),
                new Quaternionf()
        ));

        new BukkitRunnable() {
            float currentHeight = initialHeight;

            @Override
            public void run() {
                currentHeight += growSpeed;

                if (currentHeight >= targetSize) {
                    currentHeight = targetSize;
                    this.cancel();
                }

                // Adjust Y so it's always centered
                float translationY = 0.5f - (currentHeight / 2f);

                display.setTransformation(new Transformation(
                        new Vector3f((1 - width) / 2, translationY, (1 - width) / 2),
                        new Quaternionf(),
                        new Vector3f(width, currentHeight, width),
                        new Quaternionf()
                ));
            }
        }.runTaskTimer(plugin, 0L, tickInterval);
    }

    /**
     * Spawns a ring of particles around the specified center location.
     * This method is typically called when the Revive Beacon is activated or during the reviving process.
     * @param center The center location around which the ring of particles will be spawned.
     * @param particleColor The color of the particles to be spawned in the ring.
     */
    private void spawnRing(Location center, ParticleColor particleColor) {
        double radius = 1.5;
        int points = 25;
        Location ringCenter = center.clone().add(0, -0.5, 0);

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;

            center.getWorld().spawnParticle(
                    Particle.DUST,
                    ringCenter.clone().add(xOffset, 0.25, zOffset),
                    1,
                    0.0, 0.0, 0.0,
                    new Particle.DustOptions(particleColor.getColor(), 1.2f)
            );
        }
    }

    /**
     * Spawns the laser effect for a Revive Beacon at the specified location.
     *
     * @param location The location of the Revive Beacon where the laser will be spawned.
     * @param innerMaterial The material for the inner laser beam.
     * @param outerMaterial The material for the outer laser beam.
     */
    private void spawnBeaconLaser(Location location, Material innerMaterial, Material outerMaterial) {
        final float finalHeight = 150f;
        final float width1 = 0.3f;
        final float width2 = 0.5f;
        final float growSpeed = 1f; // blocks per tick

        Location quartzLoc = location.clone().add((1 - width1) / 2, 0, (1 - width1) / 2);
        Location glassLoc = location.clone().add((1 - width2) / 2, 0, (1 - width2) / 2);

        BlockDisplay quartz = location.getWorld().spawn(quartzLoc, BlockDisplay.class);
        quartz.setBlock(innerMaterial.createBlockData());
        quartz.setPersistent(true);

        BlockDisplay glass = location.getWorld().spawn(glassLoc, BlockDisplay.class);
        glass.setBlock(outerMaterial.createBlockData());
        glass.setPersistent(true);

        lasers.put(getKey(location), Set.of(quartz, glass));

        Vector3f initialQuartzScale = new Vector3f(width1, 0.1f, width1);
        Vector3f initialGlassScale = new Vector3f(width2, 0.1f, width2);
        Quaternionf noRotation = new Quaternionf();
        Vector3f translation = new Vector3f(0f, 0f, 0f);

        quartz.setTransformation(new Transformation(translation, noRotation, initialQuartzScale, noRotation));
        glass.setTransformation(new Transformation(translation, noRotation, initialGlassScale, noRotation));

        BukkitTask lasergrowTask = new BukkitRunnable() {
            float currentHeight = 0.1f;

            @Override
            public void run() {
                currentHeight += growSpeed;
                if (currentHeight >= finalHeight) {
                    currentHeight = finalHeight;
                    this.cancel();
                }

                quartz.setTransformation(new Transformation(
                        translation, noRotation, new Vector3f(width1, currentHeight, width1), noRotation
                ));
                glass.setTransformation(new Transformation(
                        translation, noRotation, new Vector3f(width2, currentHeight, width2), noRotation
                ));
            }
        }.runTaskTimer(plugin, 0L, 1L);

        laserGrowTasks.put(getKey(location), lasergrowTask);
    }

    /**
     * Stops the idle particle effects for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the particles will be stopped.
     */
    public void stopIdlePArticles(Location location) {
        BukkitTask task = idleParticleBeacons.remove(getKey(location));
        if (task != null) task.cancel();
    }

    /**
     * Stops the reviving particle effects for a Revive Beacon at the specified location.
     * @param location The location of the Revive Beacon where the particles will be stopped.
     */
    public void stopRevivingParticles(Location location) {
        BukkitTask task = revivingParticleBeacons.remove(getKey(location));
        if (task != null) task.cancel();
    }

    /**
     * Removes the laser at the specified location
     * This method is typically called when a Revive Beacon is broken or removed.
     * @param location The location of the Revive Beacon where the laser will be removed.
     */
    public void removeLaser(Location location) {
        Location key = getKey(location);

        BukkitTask growTask = laserGrowTasks.remove(key);
        if (growTask != null) growTask.cancel();

        Set<BlockDisplay> displays = lasers.remove(key);
        if (displays == null || displays.isEmpty()) return;

        final float collapseSpeed = 1f;

        new BukkitRunnable() {
            float currentHeight = displays.stream()
                    .findFirst()
                    .map(d -> d.getTransformation().getScale().y)
                    .orElse(150f);
            final float initialHeight = currentHeight;

            @Override
            public void run() {
                currentHeight -= collapseSpeed;

                if (currentHeight <= 0f) {
                    for (BlockDisplay display : displays) {
                        if (display != null) display.remove();
                    }
                    this.cancel();
                    return;
                }

                for (BlockDisplay display : displays) {
                    if (display == null) continue;

                    Vector3f originalScale = display.getTransformation().getScale();
                    float width = originalScale.x;
                    float yTranslation = (initialHeight - currentHeight) / 2f;

                    display.setTransformation(new Transformation(
                            new Vector3f(0f, yTranslation, 0f),
                            new Quaternionf(),
                            new Vector3f(width, currentHeight, width),
                            new Quaternionf()
                    ));
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Removes the decoy at the specified location.
     * @param location The location of the decoy to be removed.
     */
    public void removeDecoy(Location location) {
        BlockDisplay display = decoyDisplays.remove(getKey(location));
        if (display != null) display.remove();
    }

    /**
     * Clears all particle effects and removes the pillar at the specified location.
     * This method is typically called when a Revive Beacon is broken or removed.
     * @param location The location of the Revive Beacon where all effects will be cleared.
     */
    public void clearAllEffects(Location location) {
        stopIdlePArticles(location);
        stopRevivingParticles(location);
        removeLaser(location);
        removeDecoy(location);
        stopBossbarTask(location);
        removeBossbar(location);
    }

    /**
     * Clears all particle effects and removes all pillars.
     * This method is typically called when the plugin is disabled or when all Revive Beacons are removed.
     */
    public void clearAllEffects() {
        for (BukkitTask task : idleParticleBeacons.values()) task.cancel();
        idleParticleBeacons.clear();
        for (BukkitTask task : revivingParticleBeacons.values()) task.cancel();
        revivingParticleBeacons.clear();
        for (Set<BlockDisplay> displays : lasers.values()) {
            if (displays == null) continue;
            for (var display : displays) if (display != null) display.remove();
        }
        lasers.clear();
        for (BukkitTask task : laserGrowTasks.values()) task.cancel();
        laserGrowTasks.clear();
        for (BlockDisplay display : decoyDisplays.values()) if (display != null) display.remove();
        decoyDisplays.clear();
        for (BukkitTask task : bossbarTasks.values()) task.cancel();
        bossbarTasks.clear();
        for (BossBar bossBar : bossBars.values()) {
            if (bossBar != null) {
                bossBar.setVisible(false);
                bossBar.removeAll();
            }
        }
        bossBars.clear();
    }

    /**
     * Generates a key for the given location, ignoring the pitch and yaw.
     * @param location The location to generate a key for.
     * @return A new Location object with the same world and block coordinates, but with pitch and yaw set to 0.
     */
    private Location getKey(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Parses a BarColor from a string, returning a fallback color if the string is invalid.
     * @param color the name of the color to parse
     * @param fallbackColor the color to return if the string is invalid
     * @return the parsed color, or the fallback color if the string is invalid
     */
    private BarColor parseBossbarColor(@Nullable String color, BarColor fallbackColor) {
        try {
            if (color == null) return fallbackColor;
            return BarColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallbackColor;
        }
    }

    /**
     * Parses a BarStyle from a string, returning a fallback style if the string is invalid.
     * @param style the style to parse
     * @param fallbackStyle the style to return if the string is invalid
     * @return the parsed style, or the fallback style if the string is invalid
     */
    private BarStyle parseBossbarStyle(@Nullable String style, BarStyle fallbackStyle) {
        try {
            if (style == null) return fallbackStyle;
            return BarStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallbackStyle;
        }
    }
}
