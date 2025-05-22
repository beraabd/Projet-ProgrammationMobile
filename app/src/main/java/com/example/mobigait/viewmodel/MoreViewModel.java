package com.example.mobigait.viewmodel;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.lifecycle.AndroidViewModel;

import com.example.mobigait.model.Step;
import com.example.mobigait.model.Weight;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.repository.WeightRepository;
import com.example.mobigait.utils.UserPreferences;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoreViewModel extends AndroidViewModel {
    private static final String TAG = "MoreViewModel";

    private final StepRepository stepRepository;
    private final WeightRepository weightRepository;
    private final UserPreferences userPreferences;
    private final ExecutorService executorService;

    public MoreViewModel(@NonNull Application application) {
        super(application);
        stepRepository = new StepRepository(application);
        weightRepository = new WeightRepository(application);
        userPreferences = new UserPreferences(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public void updateWeight(float weight) {
        // Add new weight entry
        Weight newWeight = new Weight(System.currentTimeMillis(), weight);
        weightRepository.insert(newWeight);
    }

    public void updateSensorSensitivity(float threshold) {
        // Store the threshold value for step detection
        userPreferences.setSensorThreshold(threshold);
    }

    public void exportData(Context context, ExportCallback callback) {
        executorService.execute(() -> {
            try {
                // Create directory if it doesn't exist
                File exportDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "MobiGait");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                // Create file with timestamp
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
                String timestamp = dateFormat.format(new Date());
                File file = new File(exportDir, "mobigait_export_" + timestamp + ".csv");

                // Write data to file
                FileWriter writer = new FileWriter(file);

                // Write header
                writer.append("Date,Time,Steps,Distance (km),Calories,Duration (ms),Weight (kg)\n");

                // Get all step data
                List<Step> steps = stepRepository.getAllStepsSync();

                // Get all weight data
                List<Weight> weights = weightRepository.getAllWeightsSync();

                // Format for date and time
                SimpleDateFormat datePart = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat timePart = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

                // Write step data
                for (Step step : steps) {
                    Date date = new Date(step.getTimestamp());
                    writer.append(datePart.format(date)).append(",");
                    writer.append(timePart.format(date)).append(",");
                    writer.append(String.valueOf(step.getStepCount())).append(",");
                    writer.append(String.valueOf(step.getDistance())).append(",");
                    writer.append(String.valueOf(step.getCalories())).append(",");
                    writer.append(String.valueOf(step.getDuration())).append(",");
                    writer.append("\n");
                }

                // Write weight data
                for (Weight weight : weights) {
                    Date date = new Date(weight.getTimestamp());
                    writer.append(datePart.format(date)).append(",");
                    writer.append(timePart.format(date)).append(",");
                    writer.append(",,,,"); // Empty columns for steps, distance, calories, duration
                    writer.append(String.valueOf(weight.getWeight())).append("\n");
                }

                writer.flush();
                writer.close();

                // Callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> callback.onExportComplete(true, file.getAbsolutePath()));

            } catch (IOException e) {
                Log.e(TAG, "Error exporting data", e);
                // Callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                mainHandler.post(() -> callback.onExportComplete(false, null));
            }
        });
    }

    public Uri getFileUri(Context context, String filePath) {
        File file = new File(filePath);
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                file);
    }

    public void clearAllData(ClearDataCallback callback) {
        executorService.execute(() -> {
            try {
                // Clear step data
                stepRepository.deleteAllSteps();

                // Clear weight data
                weightRepository.deleteAllWeights();

                // Reset all user preferences
                userPreferences.resetAllPreferences();

                // Reset first time flag to true to trigger onboarding
                userPreferences.setFirstTime(true);

                // Callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(getApplication().getMainLooper());
                mainHandler.post(() -> callback.onClearComplete(true));

            } catch (Exception e) {
                Log.e(TAG, "Error clearing data", e);
                // Callback on main thread
                android.os.Handler mainHandler = new android.os.Handler(getApplication().getMainLooper());
                mainHandler.post(() -> callback.onClearComplete(false));
            }
        });
    }

    public interface ExportCallback {
        void onExportComplete(boolean success, String filePath);
    }

    public interface ClearDataCallback {
        void onClearComplete(boolean success);
    }
}
