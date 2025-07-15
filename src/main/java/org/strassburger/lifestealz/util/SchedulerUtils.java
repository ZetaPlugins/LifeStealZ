package org.strassburger.lifestealz.util;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.strassburger.lifestealz.LifeStealZ;

import java.util.concurrent.CompletableFuture;

public class SchedulerUtils {
    public static boolean isFolia() {
        return LifeStealZ.getFoliaLib().isFolia();
    }

    public static UniversalTask runTask(Plugin plugin, Runnable runnable) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            CompletableFuture<Void> task = LifeStealZ.getFoliaLib().getScheduler().runNextTick(wrappedTask -> runnable.run());
            return new UniversalTask(null, null, task, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return new UniversalTask(task, null, null, -1);
        }
    }

    public static UniversalTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            CompletableFuture<Void> task = LifeStealZ.getFoliaLib().getScheduler().runAsync(wrappedTask -> runnable.run());
            return new UniversalTask(null, null, task, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new UniversalTask(task, null, null, -1);
        }
    }

    public static UniversalTask runTaskLater(final Plugin plugin, final Runnable runnable, long delay) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            WrappedTask task = LifeStealZ.getFoliaLib().getScheduler().runLater(runnable, delay);
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, delay);
            return new UniversalTask(task, null, null, -1);
        }
    }

    public static UniversalTask runTaskTimerAsynchronously(final Plugin plugin, final Runnable runnable, long delay, long period) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            WrappedTask task = LifeStealZ.getFoliaLib().getScheduler().runTimerAsync(runnable, delay, period);
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delay, period);
            return new UniversalTask(task, null, null, -1);
        }
    }

    public static UniversalTask runTaskLaterAsynchronously(final Plugin plugin, final Runnable runnable, long delay) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            WrappedTask task = LifeStealZ.getFoliaLib().getScheduler().runLaterAsync(runnable, delay);
            return new UniversalTask(null, task, null, -1);
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return new UniversalTask(task, null, null, -1);
        }
    }

    public static UniversalTask scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            WrappedTask task = LifeStealZ.getFoliaLib().getScheduler().runLater(runnable, delay);
            return new UniversalTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, runnable, delay);
            return new UniversalTask(null, null, null, taskId);
        }
    }

    public static UniversalTask scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long period) {
        if (LifeStealZ.getFoliaLib().isFolia()) {
            WrappedTask task = LifeStealZ.getFoliaLib().getScheduler().runTimer(runnable, delay, period);
            return new UniversalTask(null, task, null, -1);
        } else {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, runnable, delay, period);
            return new UniversalTask(null, null, null, taskId);
        }
    }

    /**
     * 统一封装异步任务结果
     *
     * @param taskId 用于存储传统任务ID
     */
    public record UniversalTask(BukkitTask bukkitTask, WrappedTask foliaTask, CompletableFuture<Void> future, int taskId) {

        /**
         * 取消任务
         */
        public void cancel() {
            if (SchedulerUtils.isFolia()) {
                if (foliaTask != null) foliaTask.cancel();
                if (future != null) future.cancel(true);
            } else {
                if (bukkitTask != null) {
                    bukkitTask.cancel();
                } else if (taskId != -1) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }

        /**
         * 检查任务是否已取消
         */
        public boolean isCancelled() {
            if (SchedulerUtils.isFolia()) {
                return foliaTask != null && foliaTask.isCancelled();
            } else {
                if (bukkitTask != null) {
                    return bukkitTask.isCancelled();
                }
                return !Bukkit.getScheduler().isCurrentlyRunning(taskId) && !Bukkit.getScheduler().isQueued(taskId);
            }
        }
    }
}
