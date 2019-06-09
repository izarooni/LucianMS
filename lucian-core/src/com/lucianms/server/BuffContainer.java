package com.lucianms.server;

import com.lucianms.scheduler.Task;
import com.lucianms.scheduler.TaskExecutor;
import com.lucianms.server.life.MobSkill;
import tools.Disposable;

/**
 * @author izarooni
 */
public class BuffContainer implements Disposable {

    private MapleStatEffect effect;
    private MobSkill mobSkill;
    private Task task;
    private long startTime;
    private int value;

    private BuffContainer(Task task, long startTime, int value) {
        this.task = task;
        this.startTime = startTime;
        this.value = value;
    }

    public BuffContainer(MobSkill mobSkill, Task task, long startTime, int value) {
        this(task, startTime, value);
        this.mobSkill = mobSkill;
    }


    public BuffContainer(MapleStatEffect effect, Task task, long startTime, int value) {
        this(task, startTime, value);
        this.effect = effect;
    }

    @Override
    public String toString() {
        return String.format("BuffContainer{sourceID=%d,duration=%d,value=%d}", getSourceID(), getDuration(), value);
    }

    @Override
    public void dispose() {
        task = TaskExecutor.cancelTask(task);
    }

    public int getSourceID() {
        return effect == null ? mobSkill.getSkillId() : effect.getSourceId();
    }

    public int getDuration() {
        return effect == null ? (int) (mobSkill.getDuration() / 1000) : effect.getDuration();
    }

    public MapleStatEffect getEffect() {
        return effect;
    }

    public MobSkill getMobSkill() {
        return mobSkill;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
