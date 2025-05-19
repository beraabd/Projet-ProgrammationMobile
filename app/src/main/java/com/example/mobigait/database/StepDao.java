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

    @Query("SELECT * FROM steps WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    LiveData<List<Step>> getStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT SUM(stepCount) FROM steps WHERE timestamp >= :startTime AND timestamp <= :endTime")
    LiveData<Integer> getTotalStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT AVG(stepCount) FROM steps WHERE timestamp >= :startTime AND timestamp <= :endTime")
    LiveData<Double> getAverageStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT * FROM steps WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay LIMIT 1")
    Step getStepForDay(long startOfDay, long endOfDay);

    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    Step getLatestStep();

    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    LiveData<Step> getLatestStepLive();
}
