package com.example.mobigait.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.mobigait.database.AppDatabase;
import com.example.mobigait.database.StepDao;
import com.example.mobigait.model.Step;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StepRepository {
    private final StepDao stepDao;
    private final ExecutorService executorService;

    public StepRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        stepDao = database.stepDao();
        executorService = Executors.newSingleThreadExecutor();
    }
    // Add this method to the StepRepository class
    public LiveData<Step> getLatestStep() {
        return stepDao.getLatestStepLiveData();
    }


    public void insert(Step step) {
        executorService.execute(() -> stepDao.insert(step));
    }

    public void update(Step step) {
        executorService.execute(() -> stepDao.update(step));
    }

    public LiveData<List<Step>> getStepsBetweenDates(long startTime, long endTime) {
        return stepDao.getStepsBetweenDates(startTime, endTime);
    }

    public LiveData<Integer> getTotalStepsBetweenDates(long startTime, long endTime) {
        return stepDao.getTotalStepsBetweenDates(startTime, endTime);
    }

    public LiveData<Double> getAverageStepsBetweenDates(long startTime, long endTime) {
        return stepDao.getAverageStepsBetweenDates(startTime, endTime);
    }

    public void getStepForDay(long startOfDay, long endOfDay, StepCallback callback) {
        executorService.execute(() -> {
            Step step = stepDao.getStepForDay(startOfDay, endOfDay);
            callback.onStepLoaded(step);
        });
    }

    public void getLatestStepSync(StepCallback callback) {
        executorService.execute(() -> {
            Step step = stepDao.getLatestStep();
            callback.onStepLoaded(step);
        });
    }

    public interface StepCallback {
        void onStepLoaded(Step step);
    }
}
