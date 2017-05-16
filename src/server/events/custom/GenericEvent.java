package server.events.custom;

import net.PacketHandler;
import server.TimerManager;
import tools.annotation.PacketWorker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
            }
        }

        public boolean isCanceled() {
            return schedule.isCancelled();
        }
    }

    private final HashMap<Integer, Task> tasks = new HashMap<>();
    private AtomicInteger atomicInteger = new AtomicInteger(1);
    private Lock lock = new ReentrantLock(true);
    private HashMap<Class<?>, ArrayList<Method>> methods = new HashMap<>();

    /**
     * Iterates through methods of the specified object class and stores
     * found valid methods in an array list which is mapped to a packet event class
     *
     * @param object a class that contains annotated methods for packet event handlers
     */
    public final void registerAnnotationPacketEvents(Object object) {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getMethods()) {
            if (method.getAnnotation(PacketWorker.class) != null && method.getParameterCount() == 1) {
                Class<?>[] pTypes = method.getParameterTypes();
                // only add methods which parameters meet specifications
                // in this case, there is 1 parameter and it is a child of the PacketEvent
                if (PacketHandler.class.isAssignableFrom(pTypes[0])) {
                    methods.putIfAbsent(pTypes[0], new ArrayList<>());
                    methods.get(pTypes[0]).add(method);
                }
            }
        }
    }

    /**
     * Invokes all methods stored with a matching packet event parameter to this specified method call parameter.
     * <p>
     * Not to be overridden by a child class
     * </p>
     *
     * @param event a packet event
     */
    public final void onPacketEvent(PacketHandler event) {
        if (methods.get(event.getClass()) != null) {
            methods.get(event.getClass()).forEach(method -> {
                try {
                    System.out.println(String.format("[DEBUG] PacketEvent method '%s(%s)' invoked", method.getName(), event.getClass().getSimpleName()));
                    method.invoke(this, event);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public final void dispose() {
        lock.lock();
        try {
            tasks.values().forEach(Task::cancel);
        } finally {
            lock.unlock();
        }
    }

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
