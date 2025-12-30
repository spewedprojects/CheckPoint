package com.gratus.retrack;

public class RelapseLog {
    public long id;
    public long startTime;
    public long endTime;
    public long duration;
    public String reason;
    public String nextSteps;

    public RelapseLog(long startTime, long endTime, long duration, String reason, String nextSteps) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = duration;
        this.reason = reason;
        this.nextSteps = nextSteps;
    }
}