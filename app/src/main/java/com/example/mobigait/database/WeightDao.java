package com.example.mobigait.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobigait.model.Weight;

import java.util.List;

@Dao
public interface WeightDao {
    @Insert
    void insert(Weight weight);

    @Update
    void update(Weight weight);

    @Query("SELECT * FROM weights ORDER BY timestamp DESC")
    LiveData<List<Weight>> getAllWeights();

    @Query("SELECT * FROM weights ORDER BY timestamp DESC LIMIT 1")
    Weight getLatestWeight();

    @Query("SELECT * FROM weights WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<Weight>> getWeightsBetweenDates(long startTime, long endTime);
}
