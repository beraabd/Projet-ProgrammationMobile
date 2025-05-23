package com.example.mobigait.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.mobigait.database.AppDatabase;
import com.example.mobigait.database.StepDao;
import com.example.mobigait.model.Step;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StepRepository {
    private static final String TAG = "StepRepository";

    private final StepDao stepDao;
    private final ExecutorService executorService;

    public StepRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        stepDao = database.stepDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Step> getLatestStep() {
        return stepDao.getLatestStepLiveData();
    }

    public void insert(Step step) {
        executorService.execute(() -> {
            // Check if there's already a step record for today
            long startOfDay = getDayStart(step.getTimestamp());
            long endOfDay = getDayEnd(step.getTimestamp());

            Step existingStep = stepDao.getStepForDaySync(startOfDay, endOfDay);

            if (existingStep != null) {
                // Update existing record
                existingStep.setStepCount(step.getStepCount());
                existingStep.setDistance(step.getDistance());
                existingStep.setCalories(step.getCalories());
                existingStep.setDuration(step.getDuration());
                stepDao.update(existingStep);
                Log.d(TAG, "Updated existing step record: " + existingStep.getStepCount() + " steps");
            } else {
                // Insert new record
                stepDao.insert(step);
                Log.d(TAG, "Inserted new step record: " + step.getStepCount() + " steps");
            }
        });
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

    /**
     * Get the average steps between two dates
     */
    public LiveData<Double> getAverageStepsBetweenDates(long startTime, long endTime) {
        return stepDao.getAverageStepsBetweenDates(startTime, endTime);
    }

    public void getStepForDay(long startOfDay, long endOfDay, StepCallback callback) {
        executorService.execute(() -> {
            Step step = stepDao.getStepForDaySync(startOfDay, endOfDay);
            callback.onStepLoaded(step);
        });
    }

    public List<Step> getAllStepsSync() {
        // Create a single-element array to hold the result
        final List<Step>[] result = new List[1];

        // Use CountDownLatch to wait for the database operation to complete
        CountDownLatch latch = new CountDownLatch(1);

        executorService.execute(() -> {
            result[0] = stepDao.getAllStepsSync();
            latch.countDown();
        });

        try {
            // Wait for the database operation to complete (with timeout)
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Error getting all steps", e);
            Thread.currentThread().interrupt();
        }

        return result[0] != null ? result[0] : new java.util.ArrayList<>();
    }

    public void deleteAllSteps() {
        executorService.execute(() -> stepDao.deleteAllSteps());
    }

    // Helper methods for date calculations
    private long getDayStart(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getDayEnd(long timestamp) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    public interface StepCallback {
        void onStepLoaded(Step step);
    }

    public void getLatestStepSync(StepCallback callback) {
        executorService.execute(() -> {
            Step latestStep = stepDao.getLatestStepSync();
            callback.onStepLoaded(latestStep);
        });
    }

    /**
     * Get steps between two dates synchronously
     */
    public void getStepsBetweenDatesSync(long startTime, long endTime, StepDataCallback callback) {
        new Thread(() -> {
            List<Step> steps = stepDao.getStepsBetweenDatesSync(startTime, endTime);
            callback.onStepDataLoaded(steps);
        }).start();
    }

    public interface StepDataCallback {
        void onStepDataLoaded(List<Step> steps);
    }
}
