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

    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<Step>> getStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT SUM(stepCount) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    LiveData<Integer> getTotalStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT AVG(stepCount) FROM steps WHERE timestamp BETWEEN :startTime AND :endTime")
    LiveData<Double> getAverageStepsBetweenDates(long startTime, long endTime);

    @Query("SELECT * FROM steps WHERE timestamp BETWEEN :startTime AND :endTime LIMIT 1")
    Step getStepForDaySync(long startTime, long endTime);

    // Add this method to get all steps synchronously
    @Query("SELECT * FROM steps ORDER BY timestamp ASC")
    List<Step> getAllStepsSync();

    // Add this method if you need to delete all steps
    @Query("DELETE FROM steps")
    void deleteAllSteps();

    // Add this method to get the latest step as LiveData
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    LiveData<Step> getLatestStepLiveData();

    // Add this method to your StepDao interface
    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    Step getLatestStepSync();
}
