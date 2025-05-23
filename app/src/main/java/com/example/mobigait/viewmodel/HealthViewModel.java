package com.example.mobigait.viewmodel;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
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
import com.example.mobigait.sensor.GaitAnalysisService;
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
    private final MutableLiveData<String> gaitStatus = new MutableLiveData<>("Not analyzed");
    private final MutableLiveData<Float> currentWeight = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAnalyzing = new MutableLiveData<>(false);

    private LiveData<Weight> latestWeight;
    private LiveData<GaitData> latestGaitData;

    // Service connection
    private GaitAnalysisService gaitService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GaitAnalysisService.LocalBinder binder = (GaitAnalysisService.LocalBinder) service;
            gaitService = binder.getService();
            isBound = true;
            Log.d(TAG, "Gait analysis service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            gaitService = null;
            isBound = false;
            Log.d(TAG, "Gait analysis service disconnected");
        }
    };

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
                Log.d(TAG, "Updated gait status: " + gaitData.getStatus());
            }
        });

        // Initialize with current values from preferences
        float prefWeight = userPreferences.getWeight();
        if (prefWeight > 0) {
            currentWeight.setValue(prefWeight);
            calculateBmi();
        }
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

    public void startGaitAnalysis(Context context) {
        // Bind to the service if not already bound
        if (!isBound) {
            Intent serviceIntent = new Intent(context, GaitAnalysisService.class);
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // Start the analysis
        Intent serviceIntent = new Intent(context, GaitAnalysisService.class);
        serviceIntent.setAction("START_ANALYSIS");
        context.startService(serviceIntent);

        isAnalyzing.setValue(true);
        Log.d(TAG, "Started gait analysis");
    }

    public void stopGaitAnalysis(Context context) {
        if (isBound) {
            Intent serviceIntent = new Intent(context, GaitAnalysisService.class);
            serviceIntent.setAction("STOP_ANALYSIS");
            context.startService(serviceIntent);

            isAnalyzing.setValue(false);
            Log.d(TAG, "Stopped gait analysis");
        }
    }

    public void unbindGaitService(Context context) {
        if (isBound) {
            context.unbindService(serviceConnection);
            isBound = false;
            Log.d(TAG, "Unbound from gait analysis service");
        }
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

    public LiveData<List<GaitData>> getRecentGaitData() {
        return gaitRepository.getRecentGaitData(10); // Get last 10 analyses
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

    public LiveData<GaitData> getLatestGaitData() {
        return latestGaitData;
    }

    public LiveData<Boolean> isAnalyzing() {
        return isAnalyzing;
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

    // Add this method to HealthViewModel.java
    public void deleteAllGaitHistory() {
        // Use the repository to delete all gait data
        new Thread(() -> {
            // Execute deletion on a background thread
            gaitRepository.deleteAllGaitData();
        }).start();
    }
}
