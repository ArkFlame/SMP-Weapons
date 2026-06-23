package com.arkflame.smpweapons.util;

import com.arkflame.smpweapons.compat.scheduler.TaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class FoliaAPI {
    private static final TaskHandle NO_OP_TASK_HANDLE = new TaskHandle() {
        @Override
        public void cancel() {
        }
    };

    private final JavaPlugin plugin;
    private final BukkitScheduler bukkitScheduler;
    private final boolean folia;

    private final Object globalRegionScheduler;
    private final Object regionScheduler;
    private final Object asyncScheduler;

    private final Method globalRunMethod;
    private final Method globalRunDelayedMethod;
    private final Method globalRunAtFixedRateMethod;
    private final Method globalCancelTasksMethod;

    private final Method regionExecuteLocationMethod;
    private final Method regionExecuteChunkMethod;
    private final Method regionRunDelayedLocationMethod;
    private final Method regionRunAtFixedRateLocationMethod;

    private final Method entityGetSchedulerMethod;
    private final Method entityExecuteMethod;

    private final Method asyncRunNowMethod;
    private final Method asyncRunDelayedMethod;
    private final Method asyncRunAtFixedRateMethod;
    private final Method asyncCancelTasksMethod;

    private final Method playerTeleportAsyncMethod;

    public FoliaAPI(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.bukkitScheduler = Bukkit.getScheduler();
        this.playerTeleportAsyncMethod = method(Player.class, "teleportAsync", Location.class);

        final boolean foliaRuntime = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        final Object globalScheduler = foliaRuntime ? invoke(method(Server.class, "getGlobalRegionScheduler"), Bukkit.getServer()) : null;
        final Object regionScheduler = foliaRuntime ? invoke(method(Server.class, "getRegionScheduler"), Bukkit.getServer()) : null;
        final Object asyncScheduler = foliaRuntime ? invoke(method(Server.class, "getAsyncScheduler"), Bukkit.getServer()) : null;

        this.globalRegionScheduler = globalScheduler;
        this.regionScheduler = regionScheduler;
        this.asyncScheduler = asyncScheduler;
        this.folia = foliaRuntime && globalScheduler != null && regionScheduler != null && asyncScheduler != null;

        this.globalRunMethod = this.folia ? method(globalScheduler.getClass(), "run", org.bukkit.plugin.Plugin.class, Consumer.class) : null;
        this.globalRunDelayedMethod = this.folia ? method(globalScheduler.getClass(), "runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class) : null;
        this.globalRunAtFixedRateMethod = this.folia ? method(globalScheduler.getClass(), "runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class) : null;
        this.globalCancelTasksMethod = this.folia ? method(globalScheduler.getClass(), "cancelTasks", org.bukkit.plugin.Plugin.class) : null;

        this.regionExecuteLocationMethod = this.folia ? method(regionScheduler.getClass(), "execute", org.bukkit.plugin.Plugin.class, Location.class, Runnable.class) : null;
        this.regionExecuteChunkMethod = this.folia ? method(regionScheduler.getClass(), "execute", org.bukkit.plugin.Plugin.class, World.class, int.class, int.class, Runnable.class) : null;
        this.regionRunDelayedLocationMethod = this.folia ? method(regionScheduler.getClass(), "runDelayed", org.bukkit.plugin.Plugin.class, Location.class, Consumer.class, long.class) : null;
        this.regionRunAtFixedRateLocationMethod = this.folia ? method(regionScheduler.getClass(), "runAtFixedRate", org.bukkit.plugin.Plugin.class, Location.class, Consumer.class, long.class, long.class) : null;

        this.entityGetSchedulerMethod = method(Entity.class, "getScheduler");
        this.entityExecuteMethod = resolveEntityExecuteMethod();

        this.asyncRunNowMethod = this.folia ? method(asyncScheduler.getClass(), "runNow", org.bukkit.plugin.Plugin.class, Consumer.class) : null;
        this.asyncRunDelayedMethod = this.folia ? method(asyncScheduler.getClass(), "runDelayed", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, TimeUnit.class) : null;
        this.asyncRunAtFixedRateMethod = this.folia ? method(asyncScheduler.getClass(), "runAtFixedRate", org.bukkit.plugin.Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class) : null;
        this.asyncCancelTasksMethod = this.folia ? method(asyncScheduler.getClass(), "cancelTasks", org.bukkit.plugin.Plugin.class) : null;
    }

    public boolean isFolia() {
        return this.folia;
    }

    public TaskHandle runGlobal(final Runnable task) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTask(this.plugin, task));
        }
        return wrap(invoke(this.globalRunMethod, this.globalRegionScheduler, this.plugin, consumer(task)));
    }

    public TaskHandle runGlobalLater(final Runnable task, final long delayTicks) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTaskLater(this.plugin, task, delayTicks));
        }
        if (this.globalRunDelayedMethod == null) {
            return runGlobal(task);
        }
        return wrap(invoke(this.globalRunDelayedMethod, this.globalRegionScheduler, this.plugin, consumer(task), normalizePositiveTickDelay(delayTicks)));
    }

    public TaskHandle runGlobalRepeating(final Runnable task, final long delayTicks, final long periodTicks) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTaskTimer(this.plugin, task, delayTicks, periodTicks));
        }
        if (this.globalRunAtFixedRateMethod == null) {
            return NO_OP_TASK_HANDLE;
        }
        return wrap(invoke(
                this.globalRunAtFixedRateMethod,
                this.globalRegionScheduler,
                this.plugin,
                consumer(task),
                normalizePositiveTickDelay(delayTicks),
                normalizePositiveTickPeriod(periodTicks)
        ));
    }

    public TaskHandle runRegion(final Location location, final Runnable task) {
        if (location == null || task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return runGlobal(task);
        }
        if (this.regionExecuteLocationMethod != null) {
            invoke(this.regionExecuteLocationMethod, this.regionScheduler, this.plugin, location, task);
            return NO_OP_TASK_HANDLE;
        }
        return runRegion(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, task);
    }

    public TaskHandle runRegion(final World world, final int chunkX, final int chunkZ, final Runnable task) {
        if (world == null || task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return runGlobal(task);
        }
        if (this.regionExecuteChunkMethod != null) {
            invoke(this.regionExecuteChunkMethod, this.regionScheduler, this.plugin, world, Integer.valueOf(chunkX), Integer.valueOf(chunkZ), task);
            return NO_OP_TASK_HANDLE;
        }
        final Location fallbackLocation = new Location(world, (chunkX << 4) + 8.0D, world.getSpawnLocation().getY(), (chunkZ << 4) + 8.0D);
        return runRegion(fallbackLocation, task);
    }

    public TaskHandle runRegionLater(final Location location, final Runnable task, final long delayTicks) {
        if (location == null || task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return runGlobalLater(task, delayTicks);
        }
        if (this.regionRunDelayedLocationMethod != null) {
            return wrap(invoke(
                    this.regionRunDelayedLocationMethod,
                    this.regionScheduler,
                    this.plugin,
                    location,
                    consumer(task),
                    normalizePositiveTickDelay(delayTicks)
            ));
        }
        return runGlobalLater(task, delayTicks);
    }

    public TaskHandle runRegionRepeating(final Location location, final Runnable task, final long delayTicks, final long periodTicks) {
        if (location == null || task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return runGlobalRepeating(task, delayTicks, periodTicks);
        }
        if (this.regionRunAtFixedRateLocationMethod == null) {
            return runGlobalRepeating(task, delayTicks, periodTicks);
        }
        return wrap(invoke(
                this.regionRunAtFixedRateLocationMethod,
                this.regionScheduler,
                this.plugin,
                location,
                consumer(task),
                normalizePositiveTickDelay(delayTicks),
                normalizePositiveTickPeriod(periodTicks)
        ));
    }

    public TaskHandle runEntity(final Entity entity, final Runnable task) {
        return runEntityLater(entity, task, null, 0L);
    }

    public TaskHandle runEntityLater(final Entity entity, final Runnable task, final Runnable retired, final long delayTicks) {
        if (entity == null || task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            if (delayTicks <= 0L) {
                return runGlobal(task);
            }
            return runGlobalLater(task, delayTicks);
        }
        final Object entityScheduler = invoke(this.entityGetSchedulerMethod, entity);
        if (entityScheduler != null && this.entityExecuteMethod != null) {
            invoke(
                    this.entityExecuteMethod,
                    entityScheduler,
                    this.plugin,
                    task,
                    retired == null ? emptyRunnable() : retired,
                    Long.valueOf(normalizePositiveTickDelay(delayTicks))
            );
            return NO_OP_TASK_HANDLE;
        }
        return runRegionLater(entity.getLocation(), task, delayTicks);
    }

    public TaskHandle runAsync(final Runnable task) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTaskAsynchronously(this.plugin, task));
        }
        if (this.asyncRunNowMethod == null) {
            return runGlobal(task);
        }
        return wrap(invoke(this.asyncRunNowMethod, this.asyncScheduler, this.plugin, consumer(task)));
    }

    public TaskHandle runAsyncLater(final Runnable task, final long delayTicks) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTaskLaterAsynchronously(this.plugin, task, delayTicks));
        }
        if (this.asyncRunDelayedMethod == null) {
            return runAsync(task);
        }
        return wrap(invoke(
                this.asyncRunDelayedMethod,
                this.asyncScheduler,
                this.plugin,
                consumer(task),
                Long.valueOf(ticksToMillis(normalizePositiveTickDelay(delayTicks))),
                TimeUnit.MILLISECONDS
        ));
    }

    public TaskHandle runAsyncRepeating(final Runnable task, final long delayTicks, final long periodTicks) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        if (!this.folia) {
            return wrap(this.bukkitScheduler.runTaskTimerAsynchronously(this.plugin, task, delayTicks, periodTicks));
        }
        if (this.asyncRunAtFixedRateMethod == null) {
            return NO_OP_TASK_HANDLE;
        }
        return wrap(invoke(
                this.asyncRunAtFixedRateMethod,
                this.asyncScheduler,
                this.plugin,
                consumer(task),
                Long.valueOf(ticksToMillis(normalizePositiveTickDelay(delayTicks))),
                Long.valueOf(ticksToMillis(normalizePositiveTickPeriod(periodTicks))),
                TimeUnit.MILLISECONDS
        ));
    }

    public CompletableFuture<Boolean> teleport(final Player player, final Location location, final boolean async) {
        if (player == null || location == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }
        if (!this.folia) {
            if (async && this.playerTeleportAsyncMethod != null) {
                final CompletableFuture<Boolean> reflectedFuture = teleportAsync(player, location);
                if (reflectedFuture != null) {
                    return reflectedFuture;
                }
            }
            final CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
            this.bukkitScheduler.runTask(this.plugin, new Runnable() {
                @Override
                public void run() {
                    future.complete(Boolean.valueOf(player.teleport(location)));
                }
            });
            return future;
        }
        if (async) {
            final CompletableFuture<Boolean> reflectedFuture = teleportAsync(player, location);
            if (reflectedFuture != null) {
                return reflectedFuture;
            }
        }
        final CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        runEntity(player, new Runnable() {
            @Override
            public void run() {
                future.complete(Boolean.valueOf(player.teleport(location)));
            }
        });
        return future;
    }

    public void cancelAllTasks() {
        if (!this.folia) {
            this.bukkitScheduler.cancelTasks(this.plugin);
            return;
        }
        invoke(this.globalCancelTasksMethod, this.globalRegionScheduler, this.plugin);
        invoke(this.asyncCancelTasksMethod, this.asyncScheduler, this.plugin);
    }

    private CompletableFuture<Boolean> teleportAsync(final Player player, final Location location) {
        final Object result = invoke(this.playerTeleportAsyncMethod, player, location);
        if (result instanceof CompletableFuture) {
            @SuppressWarnings("unchecked") final CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) result;
            return future;
        }
        return null;
    }

    private static boolean hasClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }

    private Method resolveEntityExecuteMethod() {
        if (!this.folia || this.entityGetSchedulerMethod == null) {
            return null;
        }
        try {
            final Class<?> schedulerClass = this.entityGetSchedulerMethod.getReturnType();
            return method(schedulerClass, "execute", org.bukkit.plugin.Plugin.class, Runnable.class, Runnable.class, long.class);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Method method(final Class<?> owner, final String name, final Class<?>... parameterTypes) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Object invoke(final Method method, final Object target, final Object... args) {
        if (method == null || target == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static Consumer<Object> consumer(final Runnable task) {
        return new Consumer<Object>() {
            @Override
            public void accept(final Object ignored) {
                task.run();
            }
        };
    }

    private static Runnable emptyRunnable() {
        return new Runnable() {
            @Override
            public void run() {
            }
        };
    }

    private static long normalizePositiveTickDelay(final long delayTicks) {
        return delayTicks <= 0L ? 1L : delayTicks;
    }

    private static long normalizePositiveTickPeriod(final long periodTicks) {
        return periodTicks <= 0L ? 1L : periodTicks;
    }

    private static long ticksToMillis(final long ticks) {
        return ticks * 50L;
    }

    private static TaskHandle wrap(final BukkitTask task) {
        if (task == null) {
            return NO_OP_TASK_HANDLE;
        }
        return new TaskHandle() {
            @Override
            public void cancel() {
                task.cancel();
            }
        };
    }

    private static TaskHandle wrap(final Object handle) {
        if (handle == null) {
            return NO_OP_TASK_HANDLE;
        }
        final Method cancelMethod = method(handle.getClass(), "cancel");
        if (cancelMethod == null) {
            return NO_OP_TASK_HANDLE;
        }
        return new ReflectiveTaskHandle(handle, cancelMethod);
    }

    private static final class ReflectiveTaskHandle implements TaskHandle {
        private final Object handle;
        private final Method cancelMethod;

        private ReflectiveTaskHandle(final Object handle, final Method cancelMethod) {
            this.handle = handle;
            this.cancelMethod = cancelMethod;
        }

        @Override
        public void cancel() {
            invoke(this.cancelMethod, this.handle);
        }
    }
}
