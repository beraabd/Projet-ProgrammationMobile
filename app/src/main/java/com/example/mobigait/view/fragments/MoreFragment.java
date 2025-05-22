package com.example.mobigait.view.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.utils.UserPreferences;
import com.example.mobigait.viewmodel.MoreViewModel;

import java.util.Locale;
import com.example.mobigait.view.SplashActivity;

public class MoreFragment extends Fragment {

    private MoreViewModel viewModel;
    private UserPreferences userPreferences;

    // UI elements
    private TextView genderValue;
    private TextView heightValue;
    private TextView weightValue;
    private TextView ageValue;
    private TextView stepGoalValue;
    private TextView themeValue;
    private TextView sensitivityValue;
    private TextView versionText;

    // Settings sections
    private LinearLayout genderSetting;
    private LinearLayout heightSetting;
    private LinearLayout weightSetting;
    private LinearLayout ageSetting;
    private LinearLayout stepGoalSetting;
    private LinearLayout themeSetting;
    private LinearLayout sensitivitySetting;
    private LinearLayout instructionsSetting;
    private LinearLayout shareSetting;
    private LinearLayout privacySetting;
    private LinearLayout aboutSetting;
    private LinearLayout exportSetting;
    private LinearLayout clearDataSetting;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MoreViewModel.class);
        userPreferences = new UserPreferences(requireContext());

        // Initialize views
        initializeViews(view);
        setupListeners();
        updateUIWithUserData();
        setAppVersion();
    }

    private void initializeViews(View view) {
        // Text views
        genderValue = view.findViewById(R.id.genderValue);
        heightValue = view.findViewById(R.id.heightValue);
        weightValue = view.findViewById(R.id.weightValue);
        ageValue = view.findViewById(R.id.ageValue);
        stepGoalValue = view.findViewById(R.id.stepGoalValue);
        themeValue = view.findViewById(R.id.themeValue);
        sensitivityValue = view.findViewById(R.id.sensitivityValue);
        versionText = view.findViewById(R.id.versionText);

        // Setting sections
        genderSetting = view.findViewById(R.id.genderSetting);
        heightSetting = view.findViewById(R.id.heightSetting);
        weightSetting = view.findViewById(R.id.weightSetting);
        ageSetting = view.findViewById(R.id.ageSetting);
        stepGoalSetting = view.findViewById(R.id.stepGoalSetting);
        themeSetting = view.findViewById(R.id.themeSetting);
        sensitivitySetting = view.findViewById(R.id.sensitivitySetting);
        instructionsSetting = view.findViewById(R.id.instructionsSetting);
        shareSetting = view.findViewById(R.id.shareSetting);
        privacySetting = view.findViewById(R.id.privacySetting);
        aboutSetting = view.findViewById(R.id.aboutSetting);
        exportSetting = view.findViewById(R.id.exportSetting);
        clearDataSetting = view.findViewById(R.id.clearDataSetting);
    }

    private void setupListeners() {
        // User profile settings
        genderSetting.setOnClickListener(v -> showGenderDialog());
        heightSetting.setOnClickListener(v -> showHeightDialog());
        weightSetting.setOnClickListener(v -> showWeightDialog());
        ageSetting.setOnClickListener(v -> showAgeDialog());

        // App settings
        stepGoalSetting.setOnClickListener(v -> showStepGoalDialog());
        themeSetting.setOnClickListener(v -> showThemeDialog());
        sensitivitySetting.setOnClickListener(v -> showSensitivityDialog());

        // About & Help
        instructionsSetting.setOnClickListener(v -> showInstructions());
        shareSetting.setOnClickListener(v -> shareApp());
        privacySetting.setOnClickListener(v -> showPrivacyPolicy());
        aboutSetting.setOnClickListener(v -> showAboutDialog());

        // Data Management
        exportSetting.setOnClickListener(v -> exportData());
        clearDataSetting.setOnClickListener(v -> showClearDataDialog());
    }

    private void updateUIWithUserData() {
        // Update UI with user data from preferences
        genderValue.setText(userPreferences.getGender());
        heightValue.setText(String.format(Locale.getDefault(), "%.1f cm", userPreferences.getHeight()));
        weightValue.setText(String.format(Locale.getDefault(), "%.1f kg", userPreferences.getWeight()));
        ageValue.setText(String.format(Locale.getDefault(), "%d years", userPreferences.getAge()));
        stepGoalValue.setText(String.format(Locale.getDefault(), "%,d steps", userPreferences.getStepGoal()));

        // Theme and sensitivity are stored as strings
        themeValue.setText(userPreferences.getTheme());
        sensitivityValue.setText(userPreferences.getSensorSensitivity());
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            versionText.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("Version 1.0.0");
        }
    }

    // Dialog methods for user profile settings
    private void showGenderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Gender");

        // Inflate custom layout with radio buttons
        View view = getLayoutInflater().inflate(R.layout.dialog_gender_selection, null);
        RadioGroup radioGroup = view.findViewById(R.id.genderRadioGroup);
        RadioButton maleRadio = view.findViewById(R.id.maleRadioButton);
        RadioButton femaleRadio = view.findViewById(R.id.femaleRadioButton);

        // Set current selection
        if ("Male".equals(userPreferences.getGender())) {
            maleRadio.setChecked(true);
        } else if ("Female".equals(userPreferences.getGender())) {
            femaleRadio.setChecked(true);
        }

        builder.setView(view);

        builder.setPositiveButton("Save", (dialog, which) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.maleRadioButton) {
                userPreferences.setGender("Male");
            } else if (selectedId == R.id.femaleRadioButton) {
                userPreferences.setGender("Female");
            }
            updateUIWithUserData();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showHeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Height");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Height in cm");
        input.setText(String.valueOf(userPreferences.getHeight()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float height = Float.parseFloat(input.getText().toString());
                if (height > 0 && height < 250) {
                    userPreferences.setHeight(height);
                    updateUIWithUserData();
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid height (1-250 cm)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Weight");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Weight in kg");
        input.setText(String.valueOf(userPreferences.getWeight()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float weight = Float.parseFloat(input.getText().toString());
                if (weight > 0 && weight < 500) {
                    userPreferences.setWeight(weight);
                    // Also update weight in health tracking
                    viewModel.updateWeight(weight);
                    updateUIWithUserData();
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid weight (1-500 kg)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showAgeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Age");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Age in years");
        input.setText(String.valueOf(userPreferences.getAge()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int age = Integer.parseInt(input.getText().toString());
                if (age > 0 && age < 120) {
                    userPreferences.setAge(age);
                    updateUIWithUserData();
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid age (1-120 years)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    // Dialog methods for app settings
    private void showStepGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Set Daily Step Goal");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Step goal");
        input.setText(String.valueOf(userPreferences.getStepGoal()));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                int stepGoal = Integer.parseInt(input.getText().toString());
                if (stepGoal >= 1000 && stepGoal <= 50000) {
                    userPreferences.setStepGoal(stepGoal);
                    updateUIWithUserData();
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid goal (1,000-50,000 steps)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Theme");

        final String[] themes = {"Light", "Dark", "System Default"};
        final int currentThemeIndex;
        String currentTheme = userPreferences.getTheme();

        if ("Dark".equals(currentTheme)) {
            currentThemeIndex = 1;
        } else if ("System Default".equals(currentTheme)) {
            currentThemeIndex = 2;
        } else {
            currentThemeIndex = 0; // Light theme is default
        }

        builder.setSingleChoiceItems(themes, currentThemeIndex, (dialog, which) -> {
            userPreferences.setTheme(themes[which]);
            themeValue.setText(themes[which]);
            dialog.dismiss();

            // Show message that theme will apply on restart
            Toast.makeText(requireContext(), "Theme will apply when you restart the app", Toast.LENGTH_LONG).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    private void showSensitivityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Sensor Sensitivity");

        final String[] sensitivities = {"Low", "Medium", "High"};
        final int currentSensitivityIndex;
        String currentSensitivity = userPreferences.getSensorSensitivity();

        if ("Low".equals(currentSensitivity)) {
            currentSensitivityIndex = 0;
        } else if ("High".equals(currentSensitivity)) {
            currentSensitivityIndex = 2;
        } else {
            currentSensitivityIndex = 1; // Medium is default
        }

        builder.setSingleChoiceItems(sensitivities, currentSensitivityIndex, (dialog, which) -> {
            userPreferences.setSensorSensitivity(sensitivities[which]);
            sensitivityValue.setText(sensitivities[which]);

            // Update step detection threshold based on sensitivity
            float threshold;
            switch (which) {
                case 0: // Low
                    threshold = 15.0f;
                    break;
                case 2: // High
                    threshold = 9.0f;
                    break;
                default: // Medium
                    threshold = 12.0f;
                    break;
            }
            viewModel.updateSensorSensitivity(threshold);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // About & Help methods
    private void showInstructions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("How to Use MobiGait");

        View view = getLayoutInflater().inflate(R.layout.dialog_instructions, null);
        builder.setView(view);

        builder.setPositiveButton("Got it", null);
        builder.show();
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Check out MobiGait!");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "I'm using MobiGait to track my steps and monitor my walking health. " +
                        "Check it out: https://play.google.com/store/apps/details?id=" +
                        requireContext().getPackageName());

        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Privacy Policy");

        View view = getLayoutInflater().inflate(R.layout.dialog_privacy_policy, null);
        builder.setView(view);

        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("About MobiGait");

        View view = getLayoutInflater().inflate(R.layout.dialog_about, null);
        TextView versionTextView = view.findViewById(R.id.aboutVersionText);

        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            versionTextView.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionTextView.setText("Version 1.0.0");
        }

        builder.setView(view);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    // Data Management methods
    private void exportData() {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Exporting Data")
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Export data in background
        viewModel.exportData(requireContext(), (success, filePath) -> {
            progressDialog.dismiss();

            if (success) {
                // Show success dialog with share option
                new AlertDialog.Builder(requireContext())
                        .setTitle("Export Successful")
                        .setMessage("Data exported to:\n" + filePath)
                        .setPositiveButton("Share", (dialog, which) -> {
                            // Share the exported file
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/csv");
                            Uri fileUri = viewModel.getFileUri(requireContext(), filePath);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                            startActivity(Intent.createChooser(shareIntent, "Share exported data"));
                        })
                        .setNegativeButton("Close", null)
                        .show();
            } else {
                // Show error dialog
                new AlertDialog.Builder(requireContext())
                        .setTitle("Export Failed")
                        .setMessage("Could not export data. Please try again.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void showClearDataDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to clear all your data? This action cannot be undone.")
                .setPositiveButton("Clear Data", (dialog, which) -> {
                    // Show confirmation dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm")
                            .setMessage("ALL your data will be permanently deleted. Are you absolutely sure?")
                            .setPositiveButton("Yes, Delete Everything", (dialog2, which2) -> {
                                // Show progress dialog
                                AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                                        .setTitle("Clearing Data")
                                        .setMessage("Please wait...")
                                        .setCancelable(false)
                                        .create();
                                progressDialog.show();

                                // Clear data in background
                                viewModel.clearAllData(success -> {
                                    progressDialog.dismiss();

                                    if (success) {
                                        Toast.makeText(requireContext(), "All data has been cleared", Toast.LENGTH_LONG).show();

                                        // Set first time flag to true to trigger onboarding
                                        userPreferences.setFirstTime(true);

                                        // Restart the app from splash screen
                                        Intent intent = new Intent(requireContext(), SplashActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                       Intent.FLAG_ACTIVITY_NEW_TASK |
                                                       Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        requireActivity().finish();
                                    } else {
                                        Toast.makeText(requireContext(), "Failed to clear data", Toast.LENGTH_LONG).show();
                                    }
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }}



