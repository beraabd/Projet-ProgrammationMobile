package com.example.mobigait.view.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.sensor.StepCounterService;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;
import com.example.mobigait.viewmodel.TodayViewModel;

import java.text.DecimalFormat;

public class TodayFragment extends Fragment {
    private static final String TAG = "TodayFragment";

    private TodayViewModel viewModel;
    private UserPreferences userPreferences;

    private TextView stepCounterValue;
    private TextView distanceValue;
    private TextView caloriesValue;
    private TextView durationValue;
    private ProgressBar stepProgressBar;
    private Button startTrackingButton;
    private TextView dateText;
    private TextView goalText;

    private boolean isTracking = false;
    private DecimalFormat df = new DecimalFormat("#.##");

    // Service connection
    private StepCounterService stepService;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            StepCounterService.LocalBinder binder = (StepCounterService.LocalBinder) service;
            stepService = binder.getService();
            isBound = true;

            // Update UI with service state
            isTracking = stepService.isTracking();
            updateButtonState();

            // Update view model with current step count
            viewModel.updateSteps(stepService.getStepCount());

            Log.d(TAG, "Service connected, tracking: " + isTracking + ", steps: " + stepService.getStepCount());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_today, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TodayViewModel.class);
        userPreferences = new UserPreferences(requireContext());

        // Initialize views
        stepCounterValue = view.findViewById(R.id.stepCounterValue);
        distanceValue = view.findViewById(R.id.distanceValue);
        caloriesValue = view.findViewById(R.id.caloriesValue);
        durationValue = view.findViewById(R.id.durationValue);
        stepProgressBar = view.findViewById(R.id.stepProgressBar);
        startTrackingButton = view.findViewById(R.id.startTrackingButton);
        dateText = view.findViewById(R.id.dateText);
        goalText = view.findViewById(R.id.goalText);

        // Set today's date
        dateText.setText(DateUtils.formatDate(System.currentTimeMillis()));

        // Set goal text
        int goal = userPreferences.getStepGoal();
        goalText.setText("Goal: " + goal + " steps");
        stepProgressBar.setMax(goal);

        // Set up button click listener
        startTrackingButton.setOnClickListener(v -> {
            if (isTracking) {
                stopTracking();
            } else {
                startTracking();
            }
        });

        // Observe ViewModel data
        observeViewModel();

        // Bind to the service
        bindStepService();
    }

    private void observeViewModel() {
        viewModel.getCurrentSteps().observe(getViewLifecycleOwner(), steps -> {
            stepCounterValue.setText(String.valueOf(steps));
            stepProgressBar.setProgress(steps);
            Log.d(TAG, "Step count updated: " + steps);
        });

        viewModel.getCurrentDistance().observe(getViewLifecycleOwner(), distance -> {
            distanceValue.setText(df.format(distance) + " km");
            Log.d(TAG, "Distance updated: " + distance);
        });

        viewModel.getCurrentCalories().observe(getViewLifecycleOwner(), calories -> {
            caloriesValue.setText(df.format(calories) + " kcal");
            Log.d(TAG, "Calories updated: " + calories);
        });

        viewModel.getCurrentDuration().observe(getViewLifecycleOwner(), duration -> {
            durationValue.setText(DateUtils.formatDuration(duration));
            Log.d(TAG, "Duration updated: " + duration);
        });

        viewModel.isTracking().observe(getViewLifecycleOwner(), tracking -> {
            isTracking = tracking;
            updateButtonState();
            Log.d(TAG, "Tracking state updated: " + tracking);
        });
    }

    private void bindStepService() {
        Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
        requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Binding to step service");
    }

    private void startTracking() {
        Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
        serviceIntent.setAction(StepCounterService.ACTION_START_TRACKING);
        requireContext().startService(serviceIntent);

        viewModel.setTracking(true);
        isTracking = true;
        updateButtonState();

        Toast.makeText(requireContext(), "Step tracking started", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Started tracking");
    }

    private void stopTracking() {
        Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
        serviceIntent.setAction(StepCounterService.ACTION_STOP_TRACKING);
        requireContext().startService(serviceIntent);

        viewModel.setTracking(false);
        isTracking = false;
        updateButtonState();

        Toast.makeText(requireContext(), "Step tracking stopped", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Stopped tracking");
    }

    private void updateButtonState() {
        if (isTracking) {
            startTrackingButton.setText("Stop Tracking");
        } else {
            startTrackingButton.setText("Start Tracking");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isBound) {
            bindStepService();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Don't unbind here to keep the service running
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBound) {
            requireContext().unbindService(serviceConnection);
            isBound = false;
            Log.d(TAG, "Unbinding from step service");
        }
    }
}
