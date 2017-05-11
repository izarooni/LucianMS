package server.events.custom;

import server.TimerManager;

import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author izarooni
 */
public class GenericEvent {

    public abstract class Task {

        private final ScheduledFuture<?> schedule;

        public Task(ScheduledFuture<?> schedule) {
            this.schedule = schedule;
        }

        public abstract int getId();

        public void cancel() {
            if (!schedule.isCancelled()) {
                schedule.cancel(true);
                tasks.remove(getId());
            }
        }

        public boolean isCanceled() {
            return schedule.isCancelled();
        }
    }

    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private AtomicInteger atomicInteger = new AtomicInteger(1);
    private Lock lock = new ReentrantLock(true);

    private Task setupTask(ScheduledFuture<?> future) {
        lock.lock();
        try {
            final int id = atomicInteger.getAndIncrement();
            Task task = new Task(future) {
                @Override
                public int getId() {
                    return id;
                }
            };
            if (!tasks.containsKey(id)) {
                tasks.put(id, task);
                return task;
            }
            throw new RuntimeException(String.format("Created task with already existing id(%d)", id));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Creates a task that executes after a specified time
     *
     * @param r a runnable interface
     * @param d the delay before the task begins execution
     * @return A {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public final Task createTask(Runnable r, long d) {
        return setupTask(TimerManager.getInstance().schedule(r, d));
    }

    /**
     * Create a task that infinitely repeats until stopped by invoking the {@link Task#cancel()} method
     *
     * @param r a runnable interface
     * @param i the time between each excution
     * @param d the delay before the task begins its first execution
     * @return A {@code Task} object which is a wrapper for the {@link ScheduledFuture} object
     */
    public final Task createRepeatingTask(Runnable r, long i, long d) {
        return setupTask(TimerManager.getInstance().register(r, i, d));
    }

    /**
     * Cancels the specified task
     *
     * @param id the id of the task to cancel
     */
    public final void cancelTask(int id) {
        lock.lock();
        try {
            if (tasks.containsKey(id)) {
                tasks.get(id).cancel();
            }
        } finally {
            lock.unlock();
        }
    }

    public final Task getTask(int id) {
        lock.lock();
        try {
            return tasks.get(id);
        } finally {
            lock.unlock();
        }
    }
}
