package com.example.mobigait.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weights")
public class Weight {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long timestamp;
    private float weight;

    public Weight(long timestamp, float weight) {
        this.timestamp = timestamp;
        this.weight = weight;
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

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
}
