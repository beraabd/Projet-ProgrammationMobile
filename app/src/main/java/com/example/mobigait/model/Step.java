package com.example.mobigait.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "steps")
public class Step {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long timestamp;
    private int stepCount;
    private double distance;
    private double calories;
    private long duration;

    public Step(long timestamp, int stepCount, double distance, double calories, long duration) {
        this.timestamp = timestamp;
        this.stepCount = stepCount;
        this.distance = distance;
        this.calories = calories;
        this.duration = duration;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void setStepCount(int stepCount) {
        this.stepCount = stepCount;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
