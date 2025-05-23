package com.example.mobigait.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mobigait.MainActivity;
import com.example.mobigait.R;
import com.example.mobigait.utils.UserPreferences;
import com.example.mobigait.model.Weight;
import com.example.mobigait.repository.WeightRepository;
import java.util.List;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DURATION = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            UserPreferences userPreferences = new UserPreferences(this);

            // Force first-time flag to true for testing (remove this in production)
            // userPreferences.setFirstTime(true);

            boolean isFirstTime = userPreferences.isFirstTime();
            boolean hasCompletedOnboarding = userPreferences.hasCompletedOnboarding();

            Log.d(TAG, "isFirstTime: " + isFirstTime);
            Log.d(TAG, "hasCompletedOnboarding: " + hasCompletedOnboarding);

            // Check if this is the first time or if onboarding is incomplete
            if (isFirstTime || !hasCompletedOnboarding) {
                Log.d(TAG, "Starting OnboardingActivity");
                // First time user, show onboarding
                Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                startActivity(intent);
            } else {
                Log.d(TAG, "Starting MainActivity");
                // Returning user, go to main activity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);

                // Check if we need to initialize weight data
                checkAndInitializeWeightData(userPreferences);
            }
            finish();
        }, SPLASH_DURATION);
    }

    private void checkAndInitializeWeightData(UserPreferences userPreferences) {
        // Run this in a background thread
        new Thread(() -> {
            WeightRepository weightRepository = new WeightRepository(getApplication());
            List<Weight> weights = weightRepository.getAllWeightsSync();

            // If there's no weight data but user has a weight in preferences
            if ((weights == null || weights.isEmpty()) && userPreferences.getWeight() > 0) {
                // Add the user's weight from preferences to the database
                Weight initialWeight = new Weight(System.currentTimeMillis(), userPreferences.getWeight());
                weightRepository.insert(initialWeight);
            }
        }).start();
    }
}
