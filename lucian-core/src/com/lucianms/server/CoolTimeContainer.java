package com.lucianms.server;

import com.lucianms.scheduler.Task;

/**
 * @author izarooni
 */
public class CoolTimeContainer {

    public int skillId;
    public long startTime, length;
    public Task task;
    public byte type;

    /**
     * {@code type} parameter may be 0 for skills or 1 for others.
     *
     * @param skillId   ID of skill in cool-time
     * @param startTime Time in milliseconds the cool-time started
     * @param length    Length of cool-time before ability to re-use
     * @param task      Task which expires the cool-time (Resets)
     * @param type      Type of cool-time
     */
    public CoolTimeContainer(int skillId, long startTime, long length, Task task, byte type) {
        this.skillId = skillId;
        this.startTime = startTime;
        this.length = length;
        this.task = task;
        this.type = type;
    }
}
