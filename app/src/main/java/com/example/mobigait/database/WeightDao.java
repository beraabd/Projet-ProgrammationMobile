package com.example.mobigait.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mobigait.model.Weight;

import java.util.List;

@Dao
public interface WeightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Weight weight);

    @Update
    void update(Weight weight);

    @Query("SELECT * FROM weights WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<Weight>> getWeightsBetweenDates(long startTime, long endTime);

    @Query("SELECT * FROM weights ORDER BY timestamp DESC LIMIT 1")
    LiveData<Weight> getLatestWeight();

    @Query("SELECT * FROM weights ORDER BY timestamp ASC")
    List<Weight> getAllWeightsSync();

    @Query("DELETE FROM weights")
    void deleteAllWeights();
}
