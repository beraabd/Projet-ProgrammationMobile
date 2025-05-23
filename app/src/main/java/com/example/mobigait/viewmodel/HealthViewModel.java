package com.example.mobigait.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.mobigait.model.GaitData;
import com.example.mobigait.model.Weight;
import com.example.mobigait.repository.GaitRepository;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.repository.WeightRepository;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthViewModel extends AndroidViewModel {
    private static final String TAG = "HealthViewModel";

    private final WeightRepository weightRepository;
    private final StepRepository stepRepository;
    private final GaitRepository gaitRepository;
    private final UserPreferences userPreferences;

    private final MutableLiveData<Float> currentBmi = new MutableLiveData<>();
    private final MutableLiveData<String> bmiCategory = new MutableLiveData<>();
    private final MutableLiveData<String> gaitStatus = new MutableLiveData<>("Normal");
    private final MutableLiveData<Float> currentWeight = new MutableLiveData<>();

    // Gait metrics
    private final MutableLiveData<Float> gaitCadence = new MutableLiveData<>();
    private final MutableLiveData<Double> gaitSymmetry = new MutableLiveData<>();
    private final MutableLiveData<Double> gaitVariability = new MutableLiveData<>();
    private final MutableLiveData<Float> gaitStepLength = new MutableLiveData<>();

    private LiveData<Weight> latestWeight;
    private LiveData<GaitData> latestGaitData;

    public HealthViewModel(@NonNull Application application) {
        super(application);
        weightRepository = new WeightRepository(application);
        stepRepository = new StepRepository(application);
        gaitRepository = new GaitRepository(application);
        userPreferences = new UserPreferences(application);

        // Get the latest weight as LiveData
        latestWeight = weightRepository.getLatestWeight();

        // Get the latest gait data
        latestGaitData = gaitRepository.getLatestGaitData();

        // Observe latest weight changes
        latestWeight.observeForever(weight -> {
            if (weight != null) {
                currentWeight.setValue(weight.getWeight());
                calculateBmi();
            } else {
                // If no weight records exist, use the weight from user preferences
                float prefWeight = userPreferences.getWeight();
                if (prefWeight > 0) {
                    currentWeight.setValue(prefWeight);
                    calculateBmi();
                }
            }
        });

        // Observe latest gait data
        latestGaitData.observeForever(gaitData -> {
            if (gaitData != null) {
                gaitStatus.setValue(gaitData.getStatus());
                gaitCadence.setValue(gaitData.getCadence());
                gaitSymmetry.setValue(gaitData.getSymmetryIndex());
                gaitVariability.setValue(gaitData.getStepVariability());
                gaitStepLength.setValue(gaitData.getStepLength());

                Log.d(TAG, "Updated gait data: " + gaitData.getStatus() +
                      ", cadence: " + gaitData.getCadence());
            }
        });

        // Initialize with current values from preferences
        float prefWeight = userPreferences.getWeight();
        if (prefWeight > 0) {
            currentWeight.setValue(prefWeight);
            calculateBmi();
        }

        analyzeGait();
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

    // Update the addWeight method to accept a Weight object
    public void addWeight(Weight newWeight) {
        weightRepository.insert(newWeight);
        currentWeight.setValue(newWeight.getWeight());
        calculateBmi();
        Log.d(TAG, "Added new weight: " + newWeight.getWeight() + "kg on date: " +
              new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(newWeight.getTimestamp())));
    }

    // Keep the existing method for backward compatibility
    public void addWeight(float weight) {
        Weight newWeight = new Weight(System.currentTimeMillis(), weight);
        weightRepository.insert(newWeight);
        currentWeight.setValue(weight);
        calculateBmi();
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

    // Add method to get recent gait data
    public LiveData<List<GaitData>> getRecentGaitData(int limit) {
        return gaitRepository.getRecentGaitData(limit);
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

    public LiveData<Float> getGaitCadence() {
        return gaitCadence;
    }

    public LiveData<Double> getGaitSymmetry() {
        return gaitSymmetry;
    }

    public LiveData<Double> getGaitVariability() {
        return gaitVariability;
    }

    public LiveData<Float> getGaitStepLength() {
        return gaitStepLength;
    }

    public float getUserHeight() {
        return userPreferences.getHeight();
    }

    // Add a method to refresh user data
    public void refreshUserData() {
        float weight = userPreferences.getWeight();
        if (weight > 0) {
            currentWeight.setValue(weight);
            calculateBmi();
        }
    }

    // Add this method to the HealthViewModel class
    public void deleteAllGaitData() {
        // Execute on a background thread
        new Thread(() -> {
            gaitRepository.deleteAllGaitData();
        }).start();
    }
}
