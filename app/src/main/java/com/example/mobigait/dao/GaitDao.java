package com.example.mobigait.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.mobigait.model.GaitData;

import java.util.List;

@Dao
public interface GaitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GaitData gaitData);

    @Query("SELECT * FROM gait_data ORDER BY timestamp DESC LIMIT 1")
    LiveData<GaitData> getLatestGaitData();

    @Query("SELECT * FROM gait_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    LiveData<List<GaitData>> getGaitDataBetweenDates(long startTime, long endTime);

    @Query("SELECT * FROM gait_data ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<GaitData>> getRecentGaitData(int limit);

    @Query("DELETE FROM gait_data")
    void deleteAllGaitData();
}