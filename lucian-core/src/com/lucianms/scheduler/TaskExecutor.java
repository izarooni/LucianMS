package com.lucianms.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author izarooni
 */
public final class TaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutor.class);
    private static ScheduledThreadPoolExecutor EXECUTOR;

    private static AtomicInteger atomicInteger = new AtomicInteger(1);

    /**
     * Upon given a scheduled task from the thread pool executor, create a wrapper and assign it a unique ID
     */
    private static Task setupTask(ScheduledFuture<?> future) {
        final int id = atomicInteger.getAndIncrement();
        return new Task(future) {
            @Override
            public int getId() {
                return id;
            }
        };
    }

    public static void initPoolSize(int poolSize) {
        EXECUTOR = new ScheduledThreadPoolExecutor(poolSize, new ThreadFactory() {
            private int threadId = 0;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "LTask" + (++threadId));
            }
        });
    }

    public static ScheduledThreadPoolExecutor getExecutor() {
        return EXECUTOR;
    }

    public static void prestartAllCoreThreads() {
        int threads = EXECUTOR.prestartAllCoreThreads();
        LOGGER.info("{} cores started", threads);
    }

    public synchronized static void execute(Runnable r) {
        EXECUTOR.execute(r);
    }

    /**
     * @param r a runnable interface
     * @param a the time in milliseconds of when to execute the task
     *
     * @return A {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public synchronized static Task runAt(Runnable r, long a) {
        return setupTask(EXECUTOR.schedule(r, a - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
    }

    /**
     * Creates a task that executes after a specified time
     *
     * @param r a runnable interface
     * @param d the delay before the task begins execution
     *
     * @return A {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public synchronized static Task createTask(Runnable r, long d) {
        return setupTask(EXECUTOR.schedule(r, d, TimeUnit.MILLISECONDS));
    }

    /**
     * Create a task that infinitely repeats until stopped by invoking the {@link Task#cancel()} method
     *
     * @param r a runnable interface
     * @param i the delay before the task begins its first execution
     * @param d the time between each execution
     *
     * @return A {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public synchronized static Task createRepeatingTask(Runnable r, long i, long d) {
        return setupTask(EXECUTOR.scheduleWithFixedDelay(r, i, d, TimeUnit.MILLISECONDS));
    }

    /**
     * Create a task that infinitely repeats until stopped by invoking the {@link Task#cancel()} method
     *
     * @param r a runnable interface
     * @param t the interval time and delay in milliseconds the task will execute
     *
     * @return a {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public synchronized static Task createRepeatingTask(Runnable r, long t) {
        return setupTask(EXECUTOR.scheduleWithFixedDelay(r, t, t, TimeUnit.MILLISECONDS));
    }

    public static Task cancelTask(Task task) {
        if (task != null) {
            task.cancel();
        }
        return null;
    }
}
