package com.example.mobigait.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.mobigait.database.AppDatabase;
import com.example.mobigait.database.WeightDao;
import com.example.mobigait.model.Weight;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeightRepository {
    private static final String TAG = "WeightRepository";

    private final WeightDao weightDao;
    private final ExecutorService executorService;

    public WeightRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        weightDao = database.weightDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Weight weight) {
        executorService.execute(() -> {
            weightDao.insert(weight);
            Log.d(TAG, "Inserted weight record: " + weight.getWeight() + " kg");
        });
    }

    public void update(Weight weight) {
        executorService.execute(() -> weightDao.update(weight));
    }

    public LiveData<List<Weight>> getWeightsBetweenDates(long startTime, long endTime) {
        return weightDao.getWeightsBetweenDates(startTime, endTime);
    }

    public LiveData<Weight> getLatestWeight() {
        return weightDao.getLatestWeight();
    }

    public List<Weight> getAllWeightsSync() {
        return weightDao.getAllWeightsSync();
    }

    public void deleteAllWeights() {
        executorService.execute(() -> weightDao.deleteAllWeights());
    }
}
