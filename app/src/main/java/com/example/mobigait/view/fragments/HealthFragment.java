package com.example.mobigait.view.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobigait.R;
import com.example.mobigait.adapter.GaitHistoryAdapter;
import com.example.mobigait.model.GaitData;
import com.example.mobigait.model.Weight;
import com.example.mobigait.sensor.GaitAnalysisService;
import com.example.mobigait.viewmodel.HealthViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthFragment extends Fragment {

    private HealthViewModel viewModel;
    private TextView bmiValue;
    private TextView bmiCategory;
    private View bmiIndicator;
    private TextView gaitStatusValue;
    private TextView gaitCadenceValue;
    private TextView gaitSymmetryValue;
    private TextView gaitVariabilityValue;
    private TextView gaitStepLengthValue;
    private TextView currentWeightValue;
    private TextView weightChangeValue;
    private LineChart weightChart;
    private Button addWeightButton;
    private Button startGaitAnalysisButton;
    private Button deleteGaitHistoryButton;
    private Button explainGaitMetricsButton;
    private ImageButton gaitInfoButton;
    private RecyclerView gaitHistoryRecyclerView;
    private GaitHistoryAdapter gaitHistoryAdapter;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable analysisTimeoutRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_health, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(HealthViewModel.class);

        // Initialize views
        initializeViews(view);

        // Setup chart
        setupWeightChart();

        // Setup recycler view for gait history
        setupGaitHistoryRecyclerView();

        // Setup button listeners
        setupButtonListeners();

        // Observe data
        observeViewModel();
    }

    // Initialize all views
    private void initializeViews(View view) {
        bmiValue = view.findViewById(R.id.bmiValue);
        bmiCategory = view.findViewById(R.id.bmiCategory);
        bmiIndicator = view.findViewById(R.id.bmiIndicator);
        gaitStatusValue = view.findViewById(R.id.gaitStatusValue);
        currentWeightValue = view.findViewById(R.id.currentWeightValue);
        weightChangeValue = view.findViewById(R.id.weightChangeValue);
        weightChart = view.findViewById(R.id.weightChart);
        addWeightButton = view.findViewById(R.id.addWeightButton);

        // Gait analysis related views
        gaitCadenceValue = view.findViewById(R.id.gaitCadenceValue);
        gaitSymmetryValue = view.findViewById(R.id.gaitSymmetryValue);
        gaitVariabilityValue = view.findViewById(R.id.gaitVariabilityValue);
        gaitStepLengthValue = view.findViewById(R.id.gaitStepLengthValue);
        startGaitAnalysisButton = view.findViewById(R.id.startGaitAnalysisButton);
        gaitInfoButton = view.findViewById(R.id.gaitInfoButton);
        deleteGaitHistoryButton = view.findViewById(R.id.deleteGaitHistoryButton);
        explainGaitMetricsButton = view.findViewById(R.id.explainGaitMetricsButton);
        gaitHistoryRecyclerView = view.findViewById(R.id.gaitHistoryRecyclerView);
    }

    private void setupButtonListeners() {
        // Weight button
        addWeightButton.setOnClickListener(v -> {
            showAddWeightDialog();
        });

        // Gait analysis button
        if (startGaitAnalysisButton != null) {
            startGaitAnalysisButton.setOnClickListener(v -> {
                toggleGaitAnalysis();
            });
        }

        // Gait info button
        if (gaitInfoButton != null) {
            gaitInfoButton.setOnClickListener(v -> {
                showGaitMetricsExplanation();
            });
        }

        // Delete gait history button
        if (deleteGaitHistoryButton != null) {
            deleteGaitHistoryButton.setOnClickListener(v -> {
                showDeleteGaitHistoryConfirmation();
            });
        }

        // Explain gait metrics button
        if (explainGaitMetricsButton != null) {
            explainGaitMetricsButton.setOnClickListener(v -> {
                showGaitMetricsExplanation();
            });
        }
    }

    private void showDeleteGaitHistoryConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Gait History")
                .setMessage("Are you sure you want to delete all gait analysis history? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.deleteAllGaitHistory();
                    Toast.makeText(requireContext(), "Gait history deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupGaitHistoryRecyclerView() {
        if (gaitHistoryRecyclerView != null) {
            gaitHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            gaitHistoryAdapter = new GaitHistoryAdapter();
            gaitHistoryRecyclerView.setAdapter(gaitHistoryAdapter);
        }
    }

    private void observeViewModel() {
        // Observe BMI data
        viewModel.getCurrentBmi().observe(getViewLifecycleOwner(), bmi -> {
            if (bmi != null) {
                bmiValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                updateBmiIndicator(bmi);

                // Set BMI value color based on category
                String category = getBmiCategory(bmi);
                int color = getBmiCategoryColor(category);
                bmiValue.setTextColor(color);
            }
        });

        // Observe BMI category
        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category -> {
            if (category != null) {
                bmiCategory.setText(category);
                int color = getBmiCategoryColor(category);
                bmiCategory.setTextColor(color);
            }
        });

        // Observe gait status
        viewModel.getGaitStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                gaitStatusValue.setText(status);

                // Set color based on status
                int statusColor;
                if (status.equals("Normal")) {
                    statusColor = Color.parseColor("#4CAF50"); // Green
                    gaitStatusValue.setText(status + " ✅");
                } else if (status.equals("Not analyzed")) {
                    statusColor = Color.GRAY;
                    gaitStatusValue.setText(status);
                } else {
                    statusColor = Color.parseColor("#F44336"); // Red
                    gaitStatusValue.setText(status + " ⚠️");
                }
                gaitStatusValue.setTextColor(statusColor);
            }
        });

        // Observe current weight
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            if (weight != null) {
                currentWeightValue.setText(String.format(Locale.getDefault(), "%.1f kg", weight));
            } else {
                currentWeightValue.setText("--");
            }
        });

        // Observe weight history for chart
        viewModel.getWeightHistory().observe(getViewLifecycleOwner(), this::updateWeightChart);

        // Observe latest gait data
        viewModel.getLatestGaitData().observe(getViewLifecycleOwner(), gaitData -> {
            if (gaitData != null) {
                updateGaitMetricsUI(gaitData);
            }
        });

        // Observe gait history
        viewModel.getRecentGaitData().observe(getViewLifecycleOwner(), gaitDataList -> {
            if (gaitDataList != null && gaitHistoryAdapter != null) {
                gaitHistoryAdapter.submitList(gaitDataList);
            }
        });

        // Observe analyzing state
        viewModel.isAnalyzing().observe(getViewLifecycleOwner(), isAnalyzing -> {
            if (isAnalyzing != null && startGaitAnalysisButton != null) {
                if (isAnalyzing) {
                    startGaitAnalysisButton.setText("Stop Analysis");
                    startGaitAnalysisButton.setBackgroundColor(Color.parseColor("#F44336")); // Red
                } else {
                    startGaitAnalysisButton.setText("Start Analysis");
                    startGaitAnalysisButton.setBackgroundColor(getResources().getColor(R.color.primary, null));
                }
            }
        });
    }

    private void updateGaitMetricsUI(GaitData gaitData) {
        if (gaitCadenceValue != null) {
            gaitCadenceValue.setText(String.format(Locale.getDefault(), "%.1f steps/min", gaitData.getCadence()));
        }

        if (gaitSymmetryValue != null) {
            gaitSymmetryValue.setText(String.format(Locale.getDefault(), "%.1f%%", gaitData.getSymmetryIndex()));
        }

        if (gaitVariabilityValue != null) {
            gaitVariabilityValue.setText(String.format(Locale.getDefault(), "%.1f ms", gaitData.getStepVariability()));
        }

        if (gaitStepLengthValue != null) {
            gaitStepLengthValue.setText(String.format(Locale.getDefault(), "%.2f m", gaitData.getStepLength()));
        }
    }

    private void toggleGaitAnalysis() {
        Boolean isAnalyzing = viewModel.isAnalyzing().getValue();

        if (isAnalyzing != null && isAnalyzing) {
            // Stop analysis
            viewModel.stopGaitAnalysis(requireContext());

            // Cancel timeout if it's running
            if (analysisTimeoutRunnable != null) {
                handler.removeCallbacks(analysisTimeoutRunnable);
            }

            Toast.makeText(requireContext(), "Gait analysis stopped", Toast.LENGTH_SHORT).show();
        } else {
            // Start analysis
            viewModel.startGaitAnalysis(requireContext());

            // Show instructions
            Toast.makeText(requireContext(),
                    "Please walk for at least 30 seconds at your normal pace",
                    Toast.LENGTH_LONG).show();

            // Set a timeout to automatically stop after 30 seconds
            analysisTimeoutRunnable = () -> {
                if (isAdded() && viewModel.isAnalyzing().getValue() == Boolean.TRUE) {
                    viewModel.stopGaitAnalysis(requireContext());
                    Toast.makeText(requireContext(), "Gait analysis complete!", Toast.LENGTH_SHORT).show();
                }
            };

            handler.postDelayed(analysisTimeoutRunnable, 30000); // 30 seconds
        }
    }

    private String getBmiCategory(float bmi) {
        if (bmi < 18.5) {
            return "Underweight";
        } else if (bmi < 25) {
            return "Normal";
        } else if (bmi < 30) {
            return "Overweight";
        } else if (bmi < 35) {
            return "Obese";
        } else {
            return "Severely Obese";
        }
    }

    private int getBmiCategoryColor(String category) {
        switch (category) {
            case "Underweight":
                return Color.rgb(100, 181, 246); // Light blue
            case "Normal":
                return Color.rgb(129, 199, 132); // Light green
            case "Overweight":
                return Color.rgb(255, 213, 79); // Yellow
            case "Obese":
                return Color.rgb(255, 138, 101); // Orange
            case "Severely Obese":
                return Color.rgb(229, 115, 115); // Red
            default:
                return Color.BLACK;
        }
    }

    private void updateBmiIndicator(float bmi) {
        // Calculate position based on BMI value
        // BMI scale typically goes from 15 to 40
        float minBmi = 15f;
        float maxBmi = 40f;
        float range = maxBmi - minBmi;

        // Clamp BMI value to our range
        float clampedBmi = Math.max(minBmi, Math.min(maxBmi, bmi));

        // Calculate percentage position
        float percentage = (clampedBmi - minBmi) / range;

        // Get parent width
        View parent = (View) bmiIndicator.getParent();
        int parentWidth = parent.getWidth();
        if (parentWidth > 0) {
            // Calculate X position
            float xPosition = percentage * parentWidth;

            // Set indicator position
            bmiIndicator.setX(xPosition - bmiIndicator.getWidth() / 2);
        }
    }

    private void setupWeightChart() {
        // Basic chart setup
        weightChart.getDescription().setEnabled(false);
        weightChart.setDrawGridBackground(false);
        // Enable scrolling and scaling
        weightChart.setTouchEnabled(true);
        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setPinchZoom(true);
        weightChart.setDoubleTapToZoomEnabled(true);
        weightChart.setHighlightPerDragEnabled(true);
        weightChart.setBackgroundColor(Color.WHITE);

        // Don't set a fixed visible range - let it be determined by the data

        // Customize legend - hide it as requested
        Legend legend = weightChart.getLegend();
        legend.setEnabled(false);

        // X-axis setup
        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(6 * 24 * 60 * 60 * 1000f); // 6 days interval
        xAxis.setLabelCount(5, true); // Force approximately 5 labels
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                // Convert float to date string
                long timestamp = (long) value;
                return dateFormat.format(new Date(timestamp));
            }
        });

        // Y-axis setup
        YAxis leftAxis = weightChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);
        leftAxis.setGranularity(1f); // 1 kg intervals
        leftAxis.setTextColor(Color.BLACK);

        YAxis rightAxis = weightChart.getAxisRight();
        rightAxis.setEnabled(false);

        // If you have no data yet
        weightChart.setNoDataText("No weight data available");
        weightChart.setNoDataTextColor(Color.BLACK);

        // Apply animation
        weightChart.animateX(1000);
    }

    private void updateWeightChart(List<Weight> weightEntries) {
        if (weightEntries == null || weightEntries.isEmpty()) {
            weightChart.setNoDataText("No weight data available");
            weightChart.invalidate();
            return;
        }

        // Sort entries by timestamp (oldest to newest)
        List<Weight> sortedEntries = new ArrayList<>(weightEntries);
        Collections.sort(sortedEntries, Comparator.comparingLong(Weight::getTimestamp));

        ArrayList<Entry> values = new ArrayList<>();

        // Find min and max weight for Y-axis scaling
        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;

        // Calculate weight change over the period
        if (sortedEntries.size() >= 2) {
            Weight oldest = sortedEntries.get(0);
            Weight newest = sortedEntries.get(sortedEntries.size() - 1);
            float change = newest.getWeight() - oldest.getWeight();

            // Update weight change text
            String prefix = change > 0 ? "+" : "";
            weightChangeValue.setText(String.format(Locale.getDefault(), "%s%.1f kg", prefix, change));

            // Set text color based on weight change
            int color = change > 0 ? Color.RED : (change < 0 ? Color.GREEN : Color.BLACK);
            weightChangeValue.setTextColor(color);
        } else {
            weightChangeValue.setText("--");
        }

        // Convert your weight entries to chart entries
        for (Weight entry : sortedEntries) {
            float weight = entry.getWeight();

            // Update min/max
            minWeight = Math.min(minWeight, weight);
            maxWeight = Math.max(maxWeight, weight);

            // X value is the timestamp, Y value is the weight
            values.add(new Entry(entry.getTimestamp(), weight));
        }

        // Add some padding to min/max for better visualization
        float padding = (maxWeight - minWeight) * 0.2f; // 20% padding
        if (padding < 2) padding = 2; // Minimum 2kg padding

        minWeight = Math.max(0, minWeight - padding); // Don't go below 0
        maxWeight = maxWeight + padding;

        // Update Y-axis limits
        weightChart.getAxisLeft().setAxisMinimum(minWeight);
        weightChart.getAxisLeft().setAxisMaximum(maxWeight);

        LineDataSet set;

        if (weightChart.getData() != null &&
                weightChart.getData().getDataSetCount() > 0) {
            set = (LineDataSet) weightChart.getData().getDataSetByIndex(0);
            set.setValues(values);
            weightChart.getData().notifyDataChanged();
            weightChart.notifyDataSetChanged();
        } else {
            // Create a new dataset
            set = new LineDataSet(values, "");  // Empty label as requested
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.2f);
            set.setDrawFilled(true);
            set.setDrawCircles(true);
            set.setCircleRadius(4f);
            set.setCircleColor(getResources().getColor(R.color.primary, null));
            set.setHighLightColor(Color.rgb(244, 117, 117));
            set.setColor(getResources().getColor(R.color.primary, null));
            set.setFillColor(getResources().getColor(R.color.primary, null));
            set.setFillAlpha(50);
            set.setDrawHorizontalHighlightIndicator(false);
            set.setFillFormatter((dataSet, dataProvider) -> weightChart.getAxisLeft().getAxisMinimum());

            // Create a data object with the data sets
            LineData data = new LineData(set);
            data.setValueTextSize(9f);
            data.setDrawValues(true);
            data.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.format(Locale.getDefault(), "%.1f", value);
                }
            });

            // Set data
            weightChart.setData(data);
        }

        // Move to the latest entry
        if (values.size() > 0) {
            weightChart.moveViewToX(values.get(values.size() - 1).getX());
        }

        weightChart.invalidate();
    }

    private void showAddWeightDialog() {
        // Create dialog layout with weight input and date selection
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Ajouter un poids");

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_weight, null);
        builder.setView(dialogView);

        // Get references to views
        final EditText weightInput = dialogView.findViewById(R.id.weightInput);
        final Button dateButton = dialogView.findViewById(R.id.dateButton);

        // Set up date selection
        final Calendar calendar = Calendar.getInstance();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Set initial date text
        dateButton.setText(dateFormat.format(calendar.getTime()));

        // Set up date picker dialog
        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateButton.setText(dateFormat.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // Set max date to today (no future dates)
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

            datePickerDialog.show();
        });

        // Set up the buttons
        builder.setPositiveButton("Ajouter", (dialog, which) -> {
            try {
                float weight = Float.parseFloat(weightInput.getText().toString());
                if (weight > 0) {
                    // Create a new Weight object with selected date timestamp
                    long timestamp = calendar.getTimeInMillis();
                    Weight newWeight = new Weight(timestamp, weight);
                    viewModel.addWeight(newWeight);
                    Toast.makeText(requireContext(), "Poids ajouté", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Veuillez entrer un poids valide", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Veuillez entrer un nombre valide", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showGaitMetricsExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Understanding Your Gait Metrics");

        // Create a custom view for the dialog
        View customView = getLayoutInflater().inflate(R.layout.dialog_gait_explanation, null);
        builder.setView(customView);

        // Show the dialog
        builder.setPositiveButton("Got it", null);
        builder.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        viewModel.refreshUserData();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Cancel any pending analysis timeout
        if (analysisTimeoutRunnable != null) {
            handler.removeCallbacks(analysisTimeoutRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unbind from the service to prevent leaks
        if (viewModel != null) {
            viewModel.unbindGaitService(requireContext());
        }
    }
}
