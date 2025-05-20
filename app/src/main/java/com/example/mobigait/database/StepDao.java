package com.example.mobigait.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobigait.model.Step;

import java.util.List;

@Dao
public interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Step step);

    @Update
    void update(Step step);

    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<Step>> getStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT SUM(stepCount) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    LiveData<Integer> getTotalStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT AVG(stepCount) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    LiveData<Double> getAverageStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC LIMIT 1")
    Step getStepForDay(long startTime, long endTime);

    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    Step getLatestStep();

    // Add this method to the StepDao interface
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    LiveData<Step> getLatestStepLiveData();

}
