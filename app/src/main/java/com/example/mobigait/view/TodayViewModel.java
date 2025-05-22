package com.example.mobigait.view;

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

public class TodayViewModel extends AndroidViewModel {
    private static final String TAG = "TodayViewModel";

    private final StepRepository repository;
    private final UserPreferences userPreferences;
    private LiveData<Step> latestStep;

    private final MutableLiveData<Integer> currentSteps = new MutableLiveData<>(0);
    private final MutableLiveData<Double> currentDistance = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> currentCalories = new MutableLiveData<>(0.0);
    private final MutableLiveData<Long> currentDuration = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isTracking = new MutableLiveData<>(false);

    // Constants
    private static final double STEP_LENGTH_FACTOR = 0.415; // Average step length is 41.5% of height

    public TodayViewModel(@NonNull Application application) {
        super(application);
        repository = new StepRepository(application);
        userPreferences = new UserPreferences(application);

        // Get the latest step data and observe it
        latestStep = repository.getLatestStep();
        latestStep.observeForever(step -> {
            if (step != null) {
                Log.d(TAG, "Latest step data loaded: " + step.getStepCount());
                currentSteps.setValue(step.getStepCount());
                currentDistance.setValue(step.getDistance());
                currentCalories.setValue(step.getCalories());
                currentDuration.setValue(step.getDuration());
            }
        });

        // Load today's step data
        loadTodayData();
    }

    private void loadTodayData() {
        long[] todayTimeRange = DateUtils.getTodayTimeRange();
        repository.getStepForDay(todayTimeRange[0], todayTimeRange[1], step -> {
            if (step != null) {
                Log.d(TAG, "Today's step data loaded: " + step.getStepCount());
                currentSteps.postValue(step.getStepCount());
                currentDistance.postValue(step.getDistance());
                currentCalories.postValue(step.getCalories());
                currentDuration.postValue(step.getDuration());
            }
        });
    }

    public void updateSteps(int steps) {
        currentSteps.setValue(steps);
        updateMetrics(steps);
    }

    private void updateMetrics(int steps) {
        // Calculate distance based on user height
        float userHeight = userPreferences.getHeight();
        float userWeight = userPreferences.getWeight();

        // Default values if user data not available
        if (userHeight <= 0) userHeight = 170;
        if (userWeight <= 0) userWeight = 70;

        // Calculate step length in meters
        double stepLength = userHeight * STEP_LENGTH_FACTOR / 100;
        double distance = steps * stepLength / 1000; // in kilometers
        currentDistance.setValue(distance);

        // Calculate calories (simple formula)
        double calories = userWeight * distance;
        currentCalories.setValue(calories);

        // Save the current data
        saveCurrentData();
    }

    public void updateDuration(long durationMs) {
        currentDuration.setValue(durationMs);
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
}
