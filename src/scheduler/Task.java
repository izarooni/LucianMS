package scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * @author izarooni
 */
public abstract class Task {

    private final ScheduledFuture<?> schedule;

    Task(ScheduledFuture<?> schedule) {
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
