package com.example.mobigait;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mobigait.sensor.GaitAnalysisService;
import com.example.mobigait.sensor.StepCounterService;
import com.example.mobigait.view.fragments.HealthFragment;
import com.example.mobigait.view.fragments.MoreFragment;
import com.example.mobigait.view.fragments.ReportsFragment;
import com.example.mobigait.view.fragments.TodayFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;
    private static final int PERMISSION_REQUEST_NOTIFICATION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request necessary permissions
        requestRequiredPermissions();

        // Set up bottom navigation
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_today) {
                selectedFragment = new TodayFragment();
            } else if (itemId == R.id.navigation_reports) {
                selectedFragment = new ReportsFragment();
            } else if (itemId == R.id.navigation_health) {
                selectedFragment = new HealthFragment();
            } else if (itemId == R.id.navigation_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TodayFragment())
                    .commit();
        }

        // Start the step counter service
        startStepCounterService();
    }

    private void requestRequiredPermissions() {
        // Request activity recognition permission for Android Q and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            }
        }

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATION);
            }
        }
    }
// Add to your MainActivity:

    private void startGaitAnalysisService() {
        Intent serviceIntent = new Intent(this, GaitAnalysisService.class);
        serviceIntent.setAction("START_ANALYSIS");
        startService(serviceIntent);
    }

// Call this method in onCreate or onResume

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Activity recognition permission granted", Toast.LENGTH_SHORT).show();
                startStepCounterService();
            } else {
                Toast.makeText(this, "Activity recognition permission denied. Step counting may not work properly.",
                        Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You won't receive step counter notifications.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startStepCounterService() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        serviceIntent.setAction(StepCounterService.ACTION_START_TRACKING);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't stop the service when the activity is destroyed
        // This allows the step counter to continue running in the background
    }
}
