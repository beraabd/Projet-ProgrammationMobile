package com.example.mobigait.sensor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.mobigait.MainActivity;
import com.example.mobigait.R;
import com.example.mobigait.model.Step;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.utils.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {

    public static final String ACTION_START_TRACKING = "com.example.mobigait.ACTION_START_TRACKING";
    public static final String ACTION_PAUSE_TRACKING = "com.example.mobigait.ACTION_PAUSE_TRACKING";
    public static final String ACTION_RESUME_TRACKING = "com.example.mobigait.ACTION_RESUME_TRACKING";
    public static final String ACTION_STOP_TRACKING = "com.example.mobigait.ACTION_STOP_TRACKING";
    public static final String ACTION_RESET_TRACKING = "com.example.mobigait.ACTION_RESET_TRACKING";
    public static final String ACTION_UPDATE_NOTIFICATION = "com.example.mobigait.ACTION_UPDATE_NOTIFICATION";

    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "StepCounterService";

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor accelerometer;
    private PowerManager.WakeLock wakeLock;
    private StepRepository repository;
    private UserPreferences userPreferences;
    private boolean useAccelerometer = false;
    private int stepCount = 0;
    private int initialStepCount = -1;
    private long startTime = 0;
    private boolean isTracking = false;
    private boolean isPaused = false;

    // For accelerometer-based step counting
    private static final float STEP_THRESHOLD = 12.0f;
    private static final int STEP_DELAY_NS = 250000000; // 250ms in nanoseconds
    private float lastMagnitude = 0;
    private long lastStepTime = 0;

    // Binder for activity binding
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        repository = new StepRepository(getApplication());
        userPreferences = new UserPreferences(this);

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (stepSensor == null) {
            useAccelerometer = true;
            Log.d(TAG, "Step counter sensor not available, using accelerometer");
        }

        // Create notification channel for Android O and above
        createNotificationChannel();

        // Acquire partial wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobiGait:StepCounterWakeLock");

        // Load the latest step count from the database
        loadLatestStepCount();
    }

    private void loadLatestStepCount() {
        repository.getLatestStepSync(step -> {
            if (step != null) {
                Log.d(TAG, "Loaded latest step count: " + step.getStepCount());
                stepCount = step.getStepCount();
                startTime = System.currentTimeMillis() - step.getDuration();
            } else {
                Log.d(TAG, "No previous step data found");
                stepCount = 0;
                startTime = System.currentTimeMillis();
                startTime = System.currentTimeMillis();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "Service received action: " + action);

            if (ACTION_START_TRACKING.equals(action)) {
                startTracking();
            } else if (ACTION_PAUSE_TRACKING.equals(action)) {
                pauseTracking();
            } else if (ACTION_RESUME_TRACKING.equals(action)) {
                resumeTracking();
            } else if (ACTION_STOP_TRACKING.equals(action)) {
                stopTracking();
            } else if (ACTION_RESET_TRACKING.equals(action)) {
                resetSteps();
            } else if (ACTION_UPDATE_NOTIFICATION.equals(action)) {
                updateNotification();
            }
        }

        // If service is killed, restart it
        return START_STICKY;
    }

    private void startTracking() {
        if (isTracking) {
            Log.d(TAG, "Already tracking, ignoring start request");
            return;
        }

        Log.d(TAG, "Starting tracking");
        isTracking = true;
        isPaused = false;

        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());

        // Register sensor listener
        registerSensorListeners();

        // Acquire wake lock
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "Acquired wake lock");
        }
    }

    private void pauseTracking() {
        if (!isTracking || isPaused) {
            Log.d(TAG, "Not tracking or already paused, ignoring pause request");
            return;
        }

        Log.d(TAG, "Pausing tracking");
        isPaused = true;

        // Unregister sensor listener to save battery
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Unregistered sensor listeners");

        // Update notification
        updateNotification();
    }

    private void resumeTracking() {
        if (!isTracking || !isPaused) {
            Log.d(TAG, "Not tracking or not paused, ignoring resume request");
            return;
        }

        Log.d(TAG, "Resuming tracking");
        isPaused = false;

        // Register sensor listeners again
        registerSensorListeners();

        // Update notification
        updateNotification();
    }

    private void registerSensorListeners() {
        if (useAccelerometer) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Registered accelerometer listener");
            }
        } else if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Registered step counter listener");
        }
    }

    private void stopTracking() {
        if (!isTracking) {
            Log.d(TAG, "Not tracking, ignoring stop request");
            return;
        }

        Log.d(TAG, "Stopping tracking");
        isTracking = false;
        isPaused = false;

        // Unregister sensor listener
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Unregistered sensor listeners");

        // Release wake lock
        if (wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Released wake lock");
        }

        // Save final step data
        updateStepData();

        // Stop foreground service
        stopForeground(true);
        Log.d(TAG, "Stopped foreground service");
    }

    private void resetSteps() {
        Log.d(TAG, "Resetting steps");
        stepCount = 0;
        initialStepCount = -1;
        startTime = System.currentTimeMillis();

        // Save reset data
        updateStepData();

        // Update notification
        updateNotification();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isPaused) {
            return; // Don't process events when paused
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // Using built-in step counter
            int rawStepCount = (int) event.values[0];

            // Initialize the base value on first reading
            if (initialStepCount == -1) {
                initialStepCount = rawStepCount - stepCount;
                Log.d(TAG, "Initialized step counter with base value: " + initialStepCount);
            }

            // Calculate steps taken since we started tracking
            int newStepCount = rawStepCount - initialStepCount;
            if (newStepCount != stepCount) {
                stepCount = newStepCount;
                Log.d(TAG, "Step count updated: " + stepCount);
                updateStepData();
            }

        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && useAccelerometer) {
            // Using accelerometer for step detection
            detectStepWithAccelerometer(event);
        }

        // Update notification periodically
        if (stepCount % 10 == 0) { // Update every 10 steps to avoid too frequent updates
            updateNotification();
        }
    }

    private void detectStepWithAccelerometer(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calculate magnitude of acceleration
        float magnitude = (float) Math.sqrt(x*x + y*y + z*z);

        // Get current time in nanoseconds
        long currentTime = System.nanoTime();

        // Detect step when acceleration crosses threshold with a peak
        if (magnitude > STEP_THRESHOLD && lastMagnitude <= STEP_THRESHOLD
                && (currentTime - lastStepTime > STEP_DELAY_NS)) {
            stepCount++;
            lastStepTime = currentTime;
            Log.d(TAG, "Step detected with accelerometer, count: " + stepCount);
            updateStepData();
        }

        lastMagnitude = magnitude;
    }

    private void updateStepData() {
        // Calculate metrics
        float height = userPreferences.getHeight();
        float weight = userPreferences.getWeight();

        // Default values if user data not available
        if (height <= 0) height = 170;
        if (weight <= 0) weight = 70;

        double stepLength = 0.415 * height / 100; // step length is 41.5% of height
        double distance = stepCount * stepLength / 1000; // in kilometers
        double calories = weight * distance; // Simple formula: weight * distance
        long duration = System.currentTimeMillis() - startTime;

        // Save to database
        Step step = new Step(System.currentTimeMillis(), stepCount, distance, calories, duration);
        repository.insert(step);
        Log.d(TAG, "Saved step data: count=" + stepCount + ", distance=" + distance +
                ", calories=" + calories + ", duration=" + duration);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service",
                    NotificationManager.IMPORTANCE_LOW);

            channel.setDescription("Tracks your steps in the background");
            channel.setShowBadge(false);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Created notification channel");
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        String statusText = isPaused ? "Tracking paused" : "Tracking your steps";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MobiGait is " + statusText)
                .setContentText("Steps: " + stepCount)
                .setSmallIcon(R.drawable.ic_home)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Updated notification with step count: " + stepCount);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service being destroyed");
        if (isTracking) {
            stopTracking();
        }
        super.onDestroy();
    }

    // Public methods for binding components
    public int getStepCount() {
        return stepCount;
    }

    public boolean isTracking() {
        return isTracking && !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }
}
