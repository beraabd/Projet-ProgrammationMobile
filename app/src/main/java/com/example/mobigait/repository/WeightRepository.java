package com.example.mobigait.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.mobigait.database.AppDatabase;
import com.example.mobigait.database.WeightDao;
import com.example.mobigait.model.Weight;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeightRepository {
    private final WeightDao weightDao;
    private final ExecutorService executorService;

    public WeightRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        weightDao = database.weightDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Weight weight) {
        executorService.execute(() -> weightDao.insert(weight));
    }

    public void update(Weight weight) {
        executorService.execute(() -> weightDao.update(weight));
    }

    public LiveData<List<Weight>> getAllWeights() {
        return weightDao.getAllWeights();
    }

    public LiveData<List<Weight>> getWeightsBetweenDates(long startTime, long endTime) {
        return weightDao.getWeightsBetweenDates(startTime, endTime);
    }

    public void getLatestWeight(WeightCallback callback) {
        executorService.execute(() -> {
            Weight weight = weightDao.getLatestWeight();
            callback.onWeightLoaded(weight);
        });
    }

    public interface WeightCallback {
        void onWeightLoaded(Weight weight);
    }
}
