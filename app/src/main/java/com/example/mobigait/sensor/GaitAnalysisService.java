package com.example.mobigait.sensor;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.mobigait.model.GaitData;
import com.example.mobigait.repository.GaitRepository;

import java.util.ArrayList;
import java.util.List;

public class GaitAnalysisService extends Service implements SensorEventListener {
    private static final String TAG = "GaitAnalysisService";
    private final IBinder binder = new LocalBinder();

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private GaitRepository gaitRepository;

    // Data collection
    private List<float[]> accelerometerData = new ArrayList<>();
    private List<float[]> gyroscopeData = new ArrayList<>();
    private List<Long> timestamps = new ArrayList<>();

    // Analysis state
    private boolean isCollecting = false;
    private boolean isWalking = false;
    private long walkingStartTime = 0;

    // Walking detection thresholds
    private static final float WALKING_ACCELERATION_THRESHOLD = 1.5f; // m/s²
    private static final int WALKING_WINDOW_SIZE = 50; // samples

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        gaitRepository = new GaitRepository(getApplication());

        Log.d(TAG, "Gait analysis service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START_ANALYSIS":
                    startAnalysis();
                    break;
                case "STOP_ANALYSIS":
                    stopAnalysis();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startAnalysis() {
        if (!isCollecting) {
            // Register sensors at high sampling rate
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);

            isCollecting = true;
            Log.d(TAG, "Started gait analysis data collection");
        }
    }

    private void stopAnalysis() {
        if (isCollecting) {
            sensorManager.unregisterListener(this);
            isCollecting = false;

            // Process collected data if we have enough
            if (isWalking && accelerometerData.size() > WALKING_WINDOW_SIZE) {
                analyzeGait();
            }

            // Clear data
            clearData();
            Log.d(TAG, "Stopped gait analysis data collection");
        }
    }

    private void clearData() {
        accelerometerData.clear();
        gyroscopeData.clear();
        timestamps.clear();
        isWalking = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isCollecting) return;

        long timestamp = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Store accelerometer data
            float[] values = new float[3];
            System.arraycopy(event.values, 0, values, 0, 3);
            accelerometerData.add(values);
            timestamps.add(timestamp);

            // Detect walking
            detectWalking(values);

            // If we have enough data, analyze periodically
            if (isWalking && accelerometerData.size() >= 500) { // ~5 seconds at 100Hz
                analyzeGait();

                // Keep a sliding window of data
                int keepSize = 100; // Keep last second of data
                accelerometerData = accelerometerData.subList(
                        accelerometerData.size() - keepSize,
                        accelerometerData.size());
                gyroscopeData = gyroscopeData.subList(
                        gyroscopeData.size() - keepSize,
                        gyroscopeData.size());
                timestamps = timestamps.subList(
                        timestamps.size() - keepSize,
                        timestamps.size());
            }
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Store gyroscope data
            float[] values = new float[3];
            System.arraycopy(event.values, 0, values, 0, 3);
            gyroscopeData.add(values);
        }
    }

    private void detectWalking(float[] accelerometerValues) {
        // Calculate magnitude of acceleration
        float magnitude = (float) Math.sqrt(
                accelerometerValues[0] * accelerometerValues[0] +
                accelerometerValues[1] * accelerometerValues[1] +
                accelerometerValues[2] * accelerometerValues[2]);

        // Remove gravity component
        magnitude = Math.abs(magnitude - SensorManager.GRAVITY_EARTH);

        // Check if magnitude exceeds walking threshold
        if (magnitude > WALKING_ACCELERATION_THRESHOLD) {
            if (!isWalking) {
                isWalking = true;
                walkingStartTime = System.currentTimeMillis();
                Log.d(TAG, "Walking detected");
            }
        } else {
            // Check if we've been not walking for a while
            if (accelerometerData.size() > WALKING_WINDOW_SIZE) {
                boolean stillWalking = false;

                // Check last few samples
                for (int i = accelerometerData.size() - WALKING_WINDOW_SIZE;
                     i < accelerometerData.size(); i++) {
                    float[] values = accelerometerData.get(i);
                    float mag = (float) Math.sqrt(
                            values[0] * values[0] +
                            values[1] * values[1] +
                            values[2] * values[2]);
                    mag = Math.abs(mag - SensorManager.GRAVITY_EARTH);

                    if (mag > WALKING_ACCELERATION_THRESHOLD) {
                        stillWalking = true;
                        break;
                    }
                }

                if (!stillWalking && isWalking) {
                    isWalking = false;
                    Log.d(TAG, "Walking stopped");

                    // If we walked for at least 10 seconds, analyze the gait
                    if (System.currentTimeMillis() - walkingStartTime > 10000) {
                        analyzeGait();
                    }

                    clearData();
                }
            }
        }
    }

    private void analyzeGait() {
        if (accelerometerData.size() < WALKING_WINDOW_SIZE ||
            gyroscopeData.size() < WALKING_WINDOW_SIZE) {
            Log.d(TAG, "Not enough data to analyze gait");
            return;
        }

        Log.d(TAG, "Analyzing gait with " + accelerometerData.size() + " samples");

        // Extract features from the walking data
        GaitFeatures features = extractGaitFeatures();

        // Classify gait based on features
        String gaitStatus = classifyGait(features);

        // Save results
        GaitData gaitData = new GaitData(
                System.currentTimeMillis(),
                gaitStatus,
                features.cadence,
                features.stepVariability,
                features.symmetryIndex,
                features.stepLength
        );

        gaitRepository.insertGaitData(gaitData);
        Log.d(TAG, "Gait analysis complete: " + gaitStatus);
    }

    private GaitFeatures extractGaitFeatures() {
        GaitFeatures features = new GaitFeatures();

        // 1. Detect steps using peak detection on vertical acceleration
        List<Integer> stepIndices = detectSteps();

        // 2. Calculate cadence (steps per minute)
        if (stepIndices.size() >= 2) {
            long firstStepTime = timestamps.get(stepIndices.get(0));
            long lastStepTime = timestamps.get(stepIndices.get(stepIndices.size() - 1));
            float walkingTimeMinutes = (lastStepTime - firstStepTime) / 60000f;

            if (walkingTimeMinutes > 0) {
                features.cadence = stepIndices.size() / walkingTimeMinutes;
            }
        }

        // 3. Calculate step variability (consistency)
        if (stepIndices.size() >= 3) {
            List<Long> stepIntervals = new ArrayList<>();
            for (int i = 1; i < stepIndices.size(); i++) {
                stepIntervals.add(timestamps.get(stepIndices.get(i)) -
                                 timestamps.get(stepIndices.get(i-1)));
            }

            // Calculate standard deviation of step intervals
            double mean = 0;
            for (Long interval : stepIntervals) {
                mean += interval;
            }
            mean /= stepIntervals.size();

            double variance = 0;
            for (Long interval : stepIntervals) {
                variance += Math.pow(interval - mean, 2);
            }
            variance /= stepIntervals.size();

            features.stepVariability = Math.sqrt(variance);
        }

        // 4. Calculate symmetry index (left vs right steps)
        // This is simplified - a real implementation would need to identify left vs right steps
        if (stepIndices.size() >= 4) {
            List<Long> evenStepIntervals = new ArrayList<>();
            List<Long> oddStepIntervals = new ArrayList<>();

            for (int i = 1; i < stepIndices.size(); i++) {
                long interval = timestamps.get(stepIndices.get(i)) -
                               timestamps.get(stepIndices.get(i-1));

                if (i % 2 == 0) {
                    evenStepIntervals.add(interval);
                } else {
                    oddStepIntervals.add(interval);
                }
            }

            // Calculate average for even and odd steps
            double evenAvg = calculateAverage(evenStepIntervals);
            double oddAvg = calculateAverage(oddStepIntervals);

            // Symmetry index (0 = perfect symmetry)
            features.symmetryIndex = Math.abs(evenAvg - oddAvg) /
                                    ((evenAvg + oddAvg) / 2) * 100;
        }

        // 5. Estimate step length (very simplified)
        // A real implementation would use more sophisticated methods
        if (features.cadence > 0) {
            // Rough estimate based on cadence
            features.stepLength = 0.5f; // Default 50cm

            if (features.cadence < 90) {
                features.stepLength = 0.4f; // Shorter steps for slower cadence
            } else if (features.cadence > 120) {
                features.stepLength = 0.6f; // Longer steps for faster cadence
            }
        }

        return features;
    }

    private List<Integer> detectSteps() {
        List<Integer> stepIndices = new ArrayList<>();

        // Simple peak detection on vertical acceleration
        boolean lookingForPeak = true;
        float peakThreshold = 1.0f; // Adjust based on testing

        for (int i = 1; i < accelerometerData.size() - 1; i++) {
            float prevY = accelerometerData.get(i-1)[1];
            float currY = accelerometerData.get(i)[1];
            float nextY = accelerometerData.get(i+1)[1];

            // Detect peaks (local maxima)
            if (lookingForPeak && currY > prevY && currY > nextY && currY > peakThreshold) {
                stepIndices.add(i);
                lookingForPeak = false;
            }
            // Reset after finding a valley (local minima)
            else if (!lookingForPeak && currY < prevY && currY < nextY) {
                lookingForPeak = true;
            }
        }

        return stepIndices;
    }

    private double calculateAverage(List<Long> values) {
        if (values.isEmpty()) return 0;

        double sum = 0;
        for (Long value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    private String classifyGait(GaitFeatures features) {
        // Simple rule-based classification
        // In a real app, you would use a trained machine learning model

        if (features.cadence < 70 || features.cadence > 140) {
            return "Abnormal Cadence";
        }

        if (features.stepVariability > 200) { // 200ms variability
            return "Inconsistent Steps";
        }

        if (features.symmetryIndex > 20) { // 20% asymmetry
            return "Asymmetric Gait";
        }

        return "Normal";
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    public class LocalBinder extends Binder {
        public GaitAnalysisService getService() {
            return GaitAnalysisService.this;
        }
    }

    // Helper class to store gait features
    private static class GaitFeatures {
        float cadence = 0;           // steps per minute
        double stepVariability = 0;  // milliseconds (standard deviation)
        double symmetryIndex = 0;    // percentage (0 = perfect symmetry)
        float stepLength = 0;        // meters
    }
}