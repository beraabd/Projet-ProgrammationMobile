package com.example.mobigait.view.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.sensor.StepCounterService;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;
import com.example.mobigait.viewmodel.TodayViewModel;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TodayFragment extends Fragment {
    private static final String TAG = "TodayFragment";

    private TodayViewModel viewModel;
    private UserPreferences userPreferences;

    private TextView stepCounterValue;
    private TextView distanceValue;
    private TextView caloriesValue;
    private TextView durationValue;
    private ProgressBar stepProgressBar;
    private TextView dateText;
    private TextView goalText;
    private ImageButton pauseButton;
    private ImageButton dropdownButton;
    private CardView dropdownMenu;
    private View successButton, historyButton, resetButton, deactivateButton;

    private boolean isTracking = true; // Default to tracking
    private boolean isDropdownVisible = false;
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
            updatePauseButtonState();

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
        dateText = view.findViewById(R.id.dateText);
        goalText = view.findViewById(R.id.goalText);
        pauseButton = view.findViewById(R.id.pauseButton);
        dropdownButton = view.findViewById(R.id.dropdownButton);
        dropdownMenu = view.findViewById(R.id.dropdownMenu);

        // Find dropdown menu buttons
        successButton = view.findViewById(R.id.successButton);
        historyButton = view.findViewById(R.id.historyButton);
        resetButton = view.findViewById(R.id.resetButton);
        deactivateButton = view.findViewById(R.id.deactivateButton);

        // Set today's date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
        dateText.setText(dateFormat.format(new Date()));

        // Set goal text
        int goal = userPreferences.getStepGoal();
        goalText.setText("Goal: " + goal + " steps");
        stepProgressBar.setMax(goal);

        // Set up button click listeners
        setupClickListeners();

        // Observe ViewModel data
        observeViewModel();

        // Start tracking automatically
        startTracking();

        // Bind to the service
        bindStepService();
    }

    private void setupClickListeners() {
        // Pause button
        pauseButton.setOnClickListener(v -> {
            if (isTracking) {
                pauseTracking();
            } else {
                resumeTracking();
            }
        });

        // Dropdown button
        dropdownButton.setOnClickListener(v -> toggleDropdownMenu());

        // Dropdown menu buttons
        successButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Success!", Toast.LENGTH_SHORT).show();
            toggleDropdownMenu();
        });

        historyButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "History", Toast.LENGTH_SHORT).show();
            toggleDropdownMenu();
        });

        resetButton.setOnClickListener(v -> {
            resetSteps();
            toggleDropdownMenu();
        });

        deactivateButton.setOnClickListener(v -> {
            deactivateTracking();
            toggleDropdownMenu();
        });
    }

    private void toggleDropdownMenu() {
        if (isDropdownVisible) {
            // Hide menu with animation
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(dropdownMenu, "scaleX", 1f, 0.5f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(dropdownMenu, "scaleY", 1f, 0.5f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(dropdownMenu, "alpha", 1f, 0f);

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY, alpha);
            animSet.setDuration(300);
            animSet.start();

            dropdownMenu.setVisibility(View.GONE);

            // Rotate dropdown button back
            ObjectAnimator rotation = ObjectAnimator.ofFloat(dropdownButton, "rotation", 180f, 0f);
            rotation.setDuration(300);
            rotation.start();

            isDropdownVisible = false;
        } else {
            // Show menu with animation
            dropdownMenu.setVisibility(View.VISIBLE);
            dropdownMenu.setAlpha(0f);
            dropdownMenu.setScaleX(0.5f);
            dropdownMenu.setScaleY(0.5f);

            ObjectAnimator scaleX = ObjectAnimator.ofFloat(dropdownMenu, "scaleX", 0.5f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(dropdownMenu, "scaleY", 0.5f, 1f);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(dropdownMenu, "alpha", 0f, 1f);

            AnimatorSet animSet = new AnimatorSet();
            animSet.playTogether(scaleX, scaleY, alpha);
            animSet.setInterpolator(new OvershootInterpolator());
            animSet.setDuration(300);
            animSet.start();

            // Rotate dropdown button
            ObjectAnimator rotation = ObjectAnimator.ofFloat(dropdownButton, "rotation", 0f, 180f);
            rotation.setDuration(300);
            rotation.start();

            isDropdownVisible = true;
        }
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
            Log.d(TAG, "Observed duration update: " + duration);
            if (duration != null) {
                String formattedDuration = DateUtils.formatDuration(duration);
                durationValue.setText(formattedDuration);
            }
        });

        viewModel.isTracking().observe(getViewLifecycleOwner(), tracking -> {
            isTracking = tracking;
            updatePauseButtonState();
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
        updatePauseButtonState();

        Log.d(TAG, "Started tracking");
    }

    private void pauseTracking() {
        if (isBound && stepService != null) {
            Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
            serviceIntent.setAction(StepCounterService.ACTION_PAUSE_TRACKING);
            requireContext().startService(serviceIntent);

            viewModel.setTracking(false);
            isTracking = false;
            updatePauseButtonState();

            Toast.makeText(requireContext(), "Tracking paused", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Paused tracking");
        }
    }

    private void resumeTracking() {
        if (isBound && stepService != null) {
            Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
            serviceIntent.setAction(StepCounterService.ACTION_RESUME_TRACKING);
            requireContext().startService(serviceIntent);

            viewModel.setTracking(true);
            isTracking = true;
            updatePauseButtonState();

            Toast.makeText(requireContext(), "Tracking resumed", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Resumed tracking");
        }
    }

    private void resetSteps() {
        if (isBound && stepService != null) {
            Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
            serviceIntent.setAction(StepCounterService.ACTION_RESET_TRACKING);
            requireContext().startService(serviceIntent);

            Toast.makeText(requireContext(), "Steps reset", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Reset steps");
        }
    }

    private void deactivateTracking() {
        if (isBound && stepService != null) {
            Intent serviceIntent = new Intent(requireContext(), StepCounterService.class);
            serviceIntent.setAction(StepCounterService.ACTION_STOP_TRACKING);
            requireContext().startService(serviceIntent);

            viewModel.setTracking(false);
            isTracking = false;
            updatePauseButtonState();

            Toast.makeText(requireContext(), "Tracking deactivated", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Deactivated tracking");
        }
    }

    private void updatePauseButtonState() {
        if (isTracking) {
            pauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            pauseButton.setImageResource(R.drawable.ic_play);
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
