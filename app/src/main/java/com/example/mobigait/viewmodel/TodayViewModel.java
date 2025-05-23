package com.example.mobigait.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobigait.model.Step;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;

import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TodayViewModel extends AndroidViewModel {
    private static final String TAG = "TodayViewModel";

    private final StepRepository repository;
    private final UserPreferences userPreferences;
    private final MutableLiveData<Integer> currentSteps = new MutableLiveData<>(0);
    private final MutableLiveData<Double> currentDistance = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> currentCalories = new MutableLiveData<>(0.0);
    private final MutableLiveData<Long> currentDuration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);
    private final LiveData<Step> latestStep;

    // Constants
    private static final double STEP_LENGTH_FACTOR = 0.415; // Average step length is 41.5% of height

    public TodayViewModel(@NonNull Application application) {
        super(application);
        repository = new StepRepository(application);
        userPreferences = new UserPreferences(application);
        latestStep = repository.getLatestStep();

        // Observe latest step data
        latestStep.observeForever(step -> {
            if (step != null) {
                Log.d(TAG, "Latest step data loaded: " + step.getStepCount() + " steps");
                currentSteps.setValue(step.getStepCount());
                currentDistance.setValue(step.getDistance());
                currentCalories.setValue(step.getCalories());
                currentDuration.setValue(step.getDuration());
            }
        });

        // Load today's data
        loadTodayData();
    }

    public void loadTodayData() {
        long[] todayTimeRange = DateUtils.getTodayTimeRange();
        Log.d(TAG, "Loading today's data from " + new Date(todayTimeRange[0]) + " to " + new Date(todayTimeRange[1]));

        repository.getStepForDay(todayTimeRange[0], todayTimeRange[1], step -> {
            if (step != null) {
                Log.d(TAG, "Today's step data loaded: " + step.getStepCount() + " steps, " +
                        step.getDistance() + " km, " + step.getCalories() + " kcal, " +
                        DateUtils.formatDuration(step.getDuration()));

                // Use postValue for background thread safety
                currentSteps.postValue(step.getStepCount());
                currentDistance.postValue(step.getDistance());
                currentCalories.postValue(step.getCalories());
                currentDuration.postValue(step.getDuration());
            } else {
                Log.d(TAG, "No step data found for today");
                // Reset values to 0 if no data found
                currentSteps.postValue(0);
                currentDistance.postValue(0.0);
                currentCalories.postValue(0.0);
                currentDuration.postValue(0L);
            }
        });
    }

    public void updateSteps(int steps) {
        currentSteps.setValue(steps);
        updateMetrics(steps);
    }

    private void updateMetrics(int steps) {
        // Get user data
        float userHeight = userPreferences.getHeight();
        float userWeight = userPreferences.getWeight();

        // Use defaults if user data is not available
        if (userHeight <= 0) userHeight = 170;
        if (userWeight <= 0) userWeight = 70;

        // Calculate step length based on height
        double stepLength = userHeight * STEP_LENGTH_FACTOR / 100; // in meters

        // Calculate distance in km
        double distance = steps * stepLength / 1000; // in kilometers
        currentDistance.setValue(distance);

        // Calculate calories burned
        // Simple formula: calories = weight(kg) * distance(km) * factor
        // Factor varies by gender: men ~1.0, women ~0.9
        boolean isMale = "Male".equalsIgnoreCase(userPreferences.getGender());
        double caloriesFactor = isMale ? 1.0 : 0.9;
        double calories = userWeight * distance * caloriesFactor;
        currentCalories.setValue(calories);

        // Save to database
        saveCurrentData();
    }



    private void saveCurrentData() {
        Integer steps = currentSteps.getValue();
        Double distance = currentDistance.getValue();
        Double calories = currentCalories.getValue();
        Long duration = currentDuration.getValue();

        if (steps != null && distance != null && calories != null && duration != null) {
            Step step = new Step(
                    System.currentTimeMillis(),
                    steps,
                    distance,
                    calories,
                    duration
            );
            repository.insert(step);
            Log.d(TAG, "Saved step data: " + steps + " steps");
        }
    }

    public void setTracking(boolean tracking) {
        isTracking.setValue(tracking);
        userPreferences.setTrackingActive(tracking);
    }

    // Getters for LiveData
    public LiveData<Integer> getCurrentSteps() {
        return currentSteps;
    }

    public LiveData<Double> getCurrentDistance() {
        return currentDistance;
    }

    public LiveData<Double> getCurrentCalories() {
        return currentCalories;
    }

    public LiveData<Long> getCurrentDuration() {
        return currentDuration;
    }

    public LiveData<Boolean> isTracking() {
        return isTracking;
    }

    public LiveData<Double> getWeeklyAverageSteps() {
        long[] weekTimeRange = DateUtils.getWeekTimeRange();
        return repository.getAverageStepsBetweenDates(weekTimeRange[0], weekTimeRange[1]);
    }

    public int getGoalSteps() {
        return userPreferences.getStepGoal();
    }

    // User data setters
    public void setUserHeight(float height) {
        userPreferences.setHeight(height);
    }

    public void setUserWeight(float weight) {
        userPreferences.setWeight(weight);
    }

    public void setUserGender(boolean isMale) {
        userPreferences.setGender(isMale ? "Male" : "Female");
    }

    public void setUserAge(int age) {
        userPreferences.setAge(age);
    }

    public void getStepForDay(long startOfDay, long endOfDay, StepRepository.StepCallback callback) {
        repository.getStepForDay(startOfDay, endOfDay, callback);
    }

    public void updateDistance(double distance) {
        currentDistance.setValue(distance);
    }

    public void updateCalories(double calories) {
        currentCalories.setValue(calories);
    }

    public void updateDuration(long duration) {
        currentDuration.setValue(duration);
    }
}
