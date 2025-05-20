package com.example.mobigait.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobigait.model.Weight;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.repository.WeightRepository;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;

import java.util.Calendar;
import java.util.List;

public class HealthViewModel extends AndroidViewModel {
    private static final String TAG = "HealthViewModel";

    private final WeightRepository weightRepository;
    private final StepRepository stepRepository;
    private final UserPreferences userPreferences;

    private final MutableLiveData<Float> currentBmi = new MutableLiveData<>();
    private final MutableLiveData<String> bmiCategory = new MutableLiveData<>();
    private final MutableLiveData<String> gaitStatus = new MutableLiveData<>("Normal");
    private final MutableLiveData<Float> currentWeight = new MutableLiveData<>();

    private LiveData<Weight> latestWeight;

    public HealthViewModel(@NonNull Application application) {
        super(application);
        weightRepository = new WeightRepository(application);
        stepRepository = new StepRepository(application);
        userPreferences = new UserPreferences(application);

        // Get the latest weight as LiveData
        latestWeight = weightRepository.getLatestWeight();

        loadLatestWeight();
        analyzeGait();
    }

    private void loadLatestWeight() {
        latestWeight = weightRepository.getLatestWeight();
        // Then you can observe this LiveData from your fragment/activity
    }

    private void calculateBmi() {
        Float weight = currentWeight.getValue();
        float height = userPreferences.getHeight();

        if (weight != null && height > 0) {
            // Calculate BMI: weight (kg) / (height (m))²
            float heightInMeters = height / 100;
            float bmi = weight / (heightInMeters * heightInMeters);

            Log.d(TAG, "Calculated BMI: " + bmi + " (Weight: " + weight + "kg, Height: " + height + "cm)");

            currentBmi.postValue(bmi);

            // Determine BMI category with correct thresholds
            String category;
            if (bmi < 18.5) {
                category = "Underweight";
            } else if (bmi < 25) {
                category = "Normal";
            } else if (bmi < 30) {
                category = "Overweight";
            } else if (bmi < 35) {
                category = "Obese";
            } else {
                category = "Severely Obese";
            }

            Log.d(TAG, "BMI Category: " + category);
            bmiCategory.postValue(category);
        } else {
            Log.e(TAG, "Cannot calculate BMI: weight=" + weight + ", height=" + height);
        }
    }

    private void analyzeGait() {
        // Check if user has any step data
        long[] todayTimeRange = DateUtils.getTodayTimeRange();
        stepRepository.getTotalStepsBetweenDates(todayTimeRange[0], todayTimeRange[1]).observeForever(steps -> {
            if (steps != null && steps > 100) {
                // Only set "Normal" if user has actually walked enough steps
                gaitStatus.setValue("Normal");
                Log.d(TAG, "Gait status set to Normal based on " + steps + " steps");
            } else {
                // Not enough data to determine gait status
                gaitStatus.setValue("Not enough data");
                Log.d(TAG, "Not enough step data to determine gait status");
            }
        });
    }

    public void addWeight(float weight) {
        Weight newWeight = new Weight(System.currentTimeMillis(), weight);
        weightRepository.insert(newWeight);
        currentWeight.setValue(weight);
        calculateBmi();

        // Also update in preferences
        userPreferences.setWeight(weight);
        Log.d(TAG, "Added new weight: " + weight + "kg");
    }

    public LiveData<List<Weight>> getWeightHistory() {
        // Get weights for the last 30 days
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        return weightRepository.getWeightsBetweenDates(startTime, endTime);
    }

    // Getters for LiveData
    public LiveData<Float> getCurrentBmi() {
        return currentBmi;
    }

    public LiveData<String> getBmiCategory() {
        return bmiCategory;
    }

    public LiveData<String> getGaitStatus() {
        return gaitStatus;
    }

    public LiveData<Float> getCurrentWeight() {
        return currentWeight;
    }

    public float getUserHeight() {
        return userPreferences.getHeight();
    }
}
