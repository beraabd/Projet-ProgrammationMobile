package com.example.mobigait.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gait_data")
public class GaitData {
    @PrimaryKey
    private long timestamp;

    private String status;
    private float cadence;
    private double stepVariability;
    private double symmetryIndex;
    private float stepLength;

    public GaitData(long timestamp, String status, float cadence,
                   double stepVariability, double symmetryIndex, float stepLength) {
        this.timestamp = timestamp;
        this.status = status;
        this.cadence = cadence;
        this.stepVariability = stepVariability;
        this.symmetryIndex = symmetryIndex;
        this.stepLength = stepLength;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public float getCadence() {
        return cadence;
    }

    public void setCadence(float cadence) {
        this.cadence = cadence;
    }

    public double getStepVariability() {
        return stepVariability;
    }

    public void setStepVariability(double stepVariability) {
        this.stepVariability = stepVariability;
    }

    public double getSymmetryIndex() {
        return symmetryIndex;
    }

    public void setSymmetryIndex(double symmetryIndex) {
        this.symmetryIndex = symmetryIndex;
    }

    public float getStepLength() {
        return stepLength;
    }

    public void setStepLength(float stepLength) {
        this.stepLength = stepLength;
    }
}