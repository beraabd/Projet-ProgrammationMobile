package com.example.mobigait.sensor;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mobigait.MainActivity;
import com.example.mobigait.R;
import com.example.mobigait.model.Step;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {

    public static final String ACTION_START_TRACKING = "com.example.mobigait.ACTION_START_TRACKING";
    public static final String ACTION_PAUSE_TRACKING = "com.example.mobigait.ACTION_PAUSE_TRACKING";
    public static final String ACTION_RESUME_TRACKING = "com.example.mobigait.ACTION_RESUME_TRACKING";
    public static final String ACTION_STOP_TRACKING = "com.example.mobigait.ACTION_STOP_TRACKING";
    public static final String ACTION_RESET_TRACKING = "com.example.mobigait.ACTION_RESET_TRACKING";
    public static final String ACTION_UPDATE_NOTIFICATION = "com.example.mobigait.ACTION_UPDATE_NOTIFICATION";
    public static final String ACTION_MIDNIGHT_RESET = "com.example.mobigait.ACTION_MIDNIGHT_RESET";
    public static final String ACTION_STEP_UPDATE = "com.example.mobigait.STEP_UPDATE";

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
    private int pausedStepCount = 0;
    private long startTime = 0;
    private long pausedTime = 0;
    private boolean isTracking = false;
    private boolean isPaused = false;
    private int currentDay = -1; // Store the current day to detect day changes

    // For accelerometer-based step counting
    private static final float STEP_THRESHOLD = 12.0f;
    private static final int STEP_DELAY_NS = 250000000; // 250ms in nanoseconds
    private float lastMagnitude = 0;
    private long lastStepTime = 0;

    // Handler for periodic checks
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable dayChangeChecker = new Runnable() {
        @Override
        public void run() {
            checkForDayChange();
            // Schedule next check in 1 minute
            handler.postDelayed(this, 60 * 1000);
        }
    };

    // Midnight reset receiver
    private BroadcastReceiver midnightResetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_MIDNIGHT_RESET.equals(intent.getAction())) {
                Log.d(TAG, "Received midnight reset broadcast");
                resetForNewDay();
            }
        }
    };

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

        // Store current day
        Calendar calendar = Calendar.getInstance();
        currentDay = calendar.get(Calendar.DAY_OF_YEAR);

        // Register for midnight reset broadcasts
        IntentFilter filter = new IntentFilter(ACTION_MIDNIGHT_RESET);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (API 33+)
            registerReceiver(midnightResetReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // For older Android versions
            registerReceiver(midnightResetReceiver, filter);
        }

        // Load the latest step count from the database for today
        loadTodayStepCount();

        // Schedule midnight reset alarm
        scheduleMidnightReset();

        // Start day change checker
        handler.post(dayChangeChecker);
    }

    private void loadTodayStepCount() {
        long[] todayTimeRange = DateUtils.getTodayTimeRange();
        repository.getStepsBetweenDatesSync(todayTimeRange[0], todayTimeRange[1], steps -> {
            if (steps != null && !steps.isEmpty()) {
                // Get the latest step record for today
                Step latestStep = steps.get(0);
                Log.d(TAG, "Loaded today's step count: " + latestStep.getStepCount());
                stepCount = latestStep.getStepCount();
                startTime = System.currentTimeMillis() - latestStep.getDuration();
            } else {
                Log.d(TAG, "No step data found for today");
                stepCount = 0;
                startTime = System.currentTimeMillis();
            }
        });
    }

    private void scheduleMidnightReset() {
        // Set alarm for midnight
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // Calculate time until midnight
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Next day at midnight

            Intent intent = new Intent(ACTION_MIDNIGHT_RESET);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            // Schedule the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }

            Log.d(TAG, "Scheduled midnight reset alarm for: " +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(calendar.getTime()));
        }
    }

    private void checkForDayChange() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_YEAR);

        if (currentDay != -1 && day != currentDay) {
            Log.d(TAG, "Day change detected: " + currentDay + " -> " + day);
            resetForNewDay();
            currentDay = day;
        }
    }

    private void resetForNewDay() {
        Log.d(TAG, "Resetting for new day");

        // Save current step data with end of day timestamp
        if (stepCount > 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1); // Yesterday
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);

            // Calculate metrics
            float height = userPreferences.getHeight();
            float weight = userPreferences.getWeight();

            // Default values if user data not available
            if (height <= 0) height = 170;
            if (weight <= 0) weight = 70;

            double stepLength = 0.415 * height / 100;
            double distance = stepCount * stepLength / 1000;
            double calories = weight * distance;
            long duration = calculateDurationFromSteps(stepCount);

            // Save final data for the day
            Step finalStep = new Step(calendar.getTimeInMillis(), stepCount, distance, calories, duration);
            repository.insert(finalStep);

            Log.d(TAG, "Saved final step data for previous day: " + stepCount + " steps");
        }

        // Reset counters for the new day
        stepCount = 0;
        initialStepCount = -1;
        pausedStepCount = 0;
        startTime = System.currentTimeMillis();
        pausedTime = 0;

        // Update notification
        updateNotification();

        // Broadcast that steps have been reset for a new day
        Intent broadcastIntent = new Intent("com.example.mobigait.NEW_DAY_RESET");
        sendBroadcast(broadcastIntent);

        // Schedule next midnight reset
        scheduleMidnightReset();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_START_TRACKING:
                    startTracking();
                    isPaused = false;
                    saveTrackingState();
                    break;
                case ACTION_PAUSE_TRACKING:
                    isPaused = true;
                    saveTrackingState();
                    break;
                case ACTION_RESUME_TRACKING:
                    isPaused = false;
                    saveTrackingState();
                    break;
                case ACTION_STOP_TRACKING:
                    stopTracking();
                    isPaused = true;
                    saveTrackingState();
                    break;
                case ACTION_RESET_TRACKING:
                    resetSteps();
                    break;
                case ACTION_UPDATE_NOTIFICATION:
                    updateNotification();
                    break;
            }
        }

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

        // Check if we need to reset for a new day
        checkForDayChange();

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

        // Store the current step count when pausing
        pausedStepCount = stepCount;

        // Store the elapsed time when pausing
        pausedTime = System.currentTimeMillis() - startTime;

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

        // Check if we need to reset for a new day
        checkForDayChange();

        // Reset the initial step count to force re-initialization with the current sensor value
        initialStepCount = -1;

        // Update the start time based on the paused duration
        startTime = System.currentTimeMillis() - pausedTime;

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
        // Stop self
        stopSelf();
    }

    private void resetSteps() {
        Log.d(TAG, "Resetting steps");

        // Reset counters
        stepCount = 0;
        initialStepCount = -1;
        pausedStepCount = 0;
        startTime = System.currentTimeMillis();
        pausedTime = 0;

        // Update step data in database
        updateStepData();

        // Update notification
        updateNotification();

        // Broadcast that steps have been reset
        Intent broadcastIntent = new Intent("com.example.mobigait.STEPS_RESET");
        sendBroadcast(broadcastIntent);

        // Also broadcast the step update
        broadcastStepUpdate();
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
    }

    private void detectStepWithAccelerometer(SensorEvent event) {
        // Calculate magnitude of acceleration
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        // Get current time in nanoseconds
        long now = event.timestamp;

        // Check if this is a step (peak in acceleration)
        if (magnitude > STEP_THRESHOLD && lastMagnitude <= STEP_THRESHOLD
                && now - lastStepTime > STEP_DELAY_NS) {
            // Count as a step
            stepCount++;
            lastStepTime = now;
            Log.d(TAG, "Step detected with accelerometer, count: " + stepCount);
            updateStepData();
        }

        // Save current magnitude for next comparison
        lastMagnitude = magnitude;
    }

    private void updateStepData() {
        // Calculate distance based on step count and user height
        float height = userPreferences.getHeight();
        float weight = userPreferences.getWeight();

        // Default values if user data not available
        if (height <= 0) height = 170;
        if (weight <= 0) weight = 70;

        // Calculate step length based on height (approximate formula)
        double stepLength = 0.415 * height / 100; // in meters

        // Calculate distance in kilometers
        double distance = stepCount * stepLength / 1000;

        // Calculate calories burned (simple approximation)
        double calories = weight * distance;

        // Calculate duration based on steps (100 steps = 1 minute)
        long duration = calculateDurationFromSteps(stepCount);

        // Create Step object
        Step step = new Step(System.currentTimeMillis(), stepCount, distance, calories, duration);

        // Save to database
        repository.insert(step);

        Log.d(TAG, "Saved step data: count=" + stepCount + ", distance=" + distance +
                ", calories=" + calories + ", duration=" + duration);

        // Update notification
        updateNotification();

        // Broadcast step update
        broadcastStepUpdate();
    }

    private long calculateDurationFromSteps(int steps) {
        // Convert to float for more accurate division
        float minutesFloat = (float) steps / 100.0f;
        // Convert to milliseconds (1 minute = 60,000 ms)
        return (long) (minutesFloat * 60 * 1000);
    }

    private void broadcastStepUpdate() {
        Intent intent = new Intent(ACTION_STEP_UPDATE);
        intent.putExtra("steps", stepCount);
        intent.putExtra("distance", calculateDistance(stepCount));
        intent.putExtra("calories", calculateCalories(stepCount));
        intent.putExtra("duration", calculateDurationFromSteps(stepCount));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private double calculateDistance(int steps) {
        float height = userPreferences.getHeight();
        if (height <= 0) height = 170;

        double stepLength = 0.415 * height / 100; // in meters
        return steps * stepLength / 1000; // in kilometers
    }

    private double calculateCalories(int steps) {
        float weight = userPreferences.getWeight();
        if (weight <= 0) weight = 70;

        double distance = calculateDistance(steps);
        return weight * distance;
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
            Log.d(TAG, "Notification channel created");
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        // Create notification with current step count
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MobiGait Step Counter")
                .setContentText(getNotificationText())
                .setSmallIcon(R.drawable.ic_footprint)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true);

        // Add action buttons based on tracking state
        if (isPaused) {
            // Resume action
            Intent resumeIntent = new Intent(this, StepCounterService.class);
            resumeIntent.setAction(ACTION_RESUME_TRACKING);
            PendingIntent resumePendingIntent = PendingIntent.getService(
                    this, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_play, "Resume", resumePendingIntent);
        } else {
            // Pause action
            Intent pauseIntent = new Intent(this, StepCounterService.class);
            pauseIntent.setAction(ACTION_PAUSE_TRACKING);
            PendingIntent pausePendingIntent = PendingIntent.getService(
                    this, 2, pauseIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent);
        }

        return builder.build();
    }

    private String getNotificationText() {
        if (isPaused) {
            return "Tracking paused: " + stepCount + " steps";
        } else {
            return "Tracking: " + stepCount + " steps";
        }
    }

    private void updateNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Updated notification with step count: " + stepCount);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister sensor listener
        sensorManager.unregisterListener(this);

        // Release wake lock if held
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Unregister broadcast receiver
        try {
            unregisterReceiver(midnightResetReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
        }

        // Remove handler callbacks
        handler.removeCallbacks(dayChangeChecker);

        Log.d(TAG, "Service destroyed");
    }

    // Public methods for binding activities/fragments
    public int getStepCount() {
        return stepCount;
    }

    public double getDistance() {
        return calculateDistance(stepCount);
    }

    public double getCalories() {
        return calculateCalories(stepCount);
    }

    public long getDuration() {
        return calculateDurationFromSteps(stepCount);
    }

    public boolean isTracking() {
        return isTracking;
    }

    public boolean isPaused() {
        return isPaused;
    }

    private void saveTrackingState() {
        SharedPreferences prefs = getSharedPreferences("step_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("is_paused", isPaused).apply();
        Log.d(TAG, "Saved tracking state: isPaused=" + isPaused);
    }

    private void loadTrackingState() {
        SharedPreferences prefs = getSharedPreferences("step_prefs", MODE_PRIVATE);
        isPaused = prefs.getBoolean("is_paused", false);
        Log.d(TAG, "Loaded tracking state: isPaused=" + isPaused);
    }
}
