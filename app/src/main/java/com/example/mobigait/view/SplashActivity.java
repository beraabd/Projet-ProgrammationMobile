package com.example.mobigait.view;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mobigait.MainActivity;
import com.example.mobigait.R;
import com.example.mobigait.utils.UserPreferences;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIMEOUT = 2000; // 2 seconds
    private UserPreferences userPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        userPreferences = new UserPreferences(this);

        new Handler().postDelayed(() -> {
            // Check if user has completed onboarding
            if (userPreferences.hasCompletedOnboarding()) {
                // User has completed onboarding, go to main activity
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // User has not completed onboarding, go to onboarding activity
                Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                startActivity(intent);
            }
            finish();
        }, SPLASH_TIMEOUT);
    }
}
