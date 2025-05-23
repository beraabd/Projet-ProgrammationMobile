package com.example.mobigait.view.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    private static final String TAG = "HealthFragment";

    private HealthViewModel viewModel;
    private TextView bmiValue;
    private TextView bmiCategory;
    private View bmiIndicator;
    private TextView gaitStatusValue;
    private TextView currentWeightValue;
    private TextView weightChangeValue;
    private LineChart weightChart;
    private Button addWeightButton;
    private Button startGaitAnalysisButton;

    // Gait metrics views
    private TextView gaitCadenceValue;
    private TextView gaitSymmetryValue;
    private TextView gaitVariabilityValue;
    private TextView gaitStepLengthValue;
    private RecyclerView gaitHistoryRecyclerView;
    private ImageButton gaitInfoButton;

    // Broadcast receiver for gait analysis updates
    private final BroadcastReceiver gaitAnalysisReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GaitAnalysisService.ACTION_GAIT_ANALYSIS_COMPLETED.equals(intent.getAction())) {
                // Update UI when gait analysis is completed
                if (startGaitAnalysisButton != null) {
                    startGaitAnalysisButton.setText("Start Analysis");
                    startGaitAnalysisButton.setEnabled(true);
                }

                // Show success message
                Toast.makeText(requireContext(), "Gait analysis completed!", Toast.LENGTH_SHORT).show();

                // The ViewModel will automatically update with the latest gait data
            }
        }
    };

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
        bmiValue = view.findViewById(R.id.bmiValue);
        bmiCategory = view.findViewById(R.id.bmiCategory);
        bmiIndicator = view.findViewById(R.id.bmiIndicator);
        gaitStatusValue = view.findViewById(R.id.gaitStatusValue);
        currentWeightValue = view.findViewById(R.id.currentWeightValue);
        weightChangeValue = view.findViewById(R.id.weightChangeValue);
        weightChart = view.findViewById(R.id.weightChart);
        addWeightButton = view.findViewById(R.id.addWeightButton);

        // Initialize gait metrics views
        gaitCadenceValue = view.findViewById(R.id.gaitCadenceValue);
        gaitSymmetryValue = view.findViewById(R.id.gaitSymmetryValue);
        gaitVariabilityValue = view.findViewById(R.id.gaitVariabilityValue);
        gaitStepLengthValue = view.findViewById(R.id.gaitStepLengthValue);

        // Initialize gait analysis views
        gaitHistoryRecyclerView = view.findViewById(R.id.gaitHistoryRecyclerView);
        startGaitAnalysisButton = view.findViewById(R.id.startGaitAnalysisButton);
        gaitInfoButton = view.findViewById(R.id.gaitInfoButton);

        // Initialize the explain gait metrics button
        Button explainGaitMetricsButton = view.findViewById(R.id.explainGaitMetricsButton);
        if (explainGaitMetricsButton != null) {
            explainGaitMetricsButton.setOnClickListener(v -> {
                showGaitMetricsExplanation();
            });
        }

        // Initialize the delete history button
        Button deleteGaitHistoryButton = view.findViewById(R.id.deleteGaitHistoryButton);
        if (deleteGaitHistoryButton != null) {
            deleteGaitHistoryButton.setOnClickListener(v -> {
                showDeleteGaitHistoryConfirmation();
            });
        }

        // Setup chart
        setupWeightChart();
        // Setup button listeners
        addWeightButton.setOnClickListener(v -> {
            showAddWeightDialog();
        });

        if (startGaitAnalysisButton != null) {
            startGaitAnalysisButton.setOnClickListener(v -> {
                // Show instructions to the user
                Toast.makeText(requireContext(),
                    "Walk normally for 30 seconds. Analysis will complete automatically.",
                    Toast.LENGTH_LONG).show();

                // Start the gait analysis service
                Intent serviceIntent = new Intent(requireContext(), GaitAnalysisService.class);
                serviceIntent.setAction("START_ANALYSIS");
                requireContext().startService(serviceIntent);

                // Change button text to indicate analysis is in progress
                startGaitAnalysisButton.setText("Analysis in progress...");
                startGaitAnalysisButton.setEnabled(false);

                // Re-enable the button after 30 seconds if no response is received
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (startGaitAnalysisButton.getText().toString().contains("progress")) {
                        startGaitAnalysisButton.setText("Start Analysis");
                        startGaitAnalysisButton.setEnabled(true);
                        Toast.makeText(requireContext(), "Analysis timed out. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }, 30000); // 30 seconds
            });
        }

        if (gaitInfoButton != null) {
            gaitInfoButton.setOnClickListener(v -> {
                showGaitMetricsExplanation();
            });
        }

        // Setup RecyclerView for gait history
        if (gaitHistoryRecyclerView != null) {
            gaitHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            GaitHistoryAdapter adapter = new GaitHistoryAdapter();
            gaitHistoryRecyclerView.setAdapter(adapter);

            // Observe gait history data
            viewModel.getRecentGaitData(5).observe(getViewLifecycleOwner(), gaitDataList -> {
                if (gaitDataList != null) {
                    adapter.submitList(gaitDataList);

                    // Show a toast if no data is available
                    if (gaitDataList.isEmpty() && startGaitAnalysisButton != null) {
                        // Make sure the button is visible and enabled
                        startGaitAnalysisButton.setEnabled(true);
                        startGaitAnalysisButton.setText("Start Analysis");
                    }
                }
            });

            // Set click listener for detailed view
            adapter.setOnGaitItemClickListener(gaitData -> {
                // Show detailed gait data dialog
                showGaitDetailDialog(gaitData);
            });
        }

        // Register broadcast receiver for gait analysis updates
        IntentFilter filter = new IntentFilter(GaitAnalysisService.ACTION_GAIT_ANALYSIS_COMPLETED);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(gaitAnalysisReceiver, filter);

        // Observe data
        observeViewModel();
    }

    private void updateGaitMetrics(String status, float cadence, double variability, double symmetry, float stepLength) {
        if (gaitStatusValue != null) {
            gaitStatusValue.setText(status);

            // Set color based on status
            int color;
            if ("Normal".equals(status)) {
                color = getResources().getColor(R.color.primary, null);
                gaitStatusValue.setText(status + " ✅");
            } else {
                color = Color.RED;
                gaitStatusValue.setText(status + " ⚠️");
            }
            gaitStatusValue.setTextColor(color);
        }

        if (gaitCadenceValue != null) {
            gaitCadenceValue.setText(String.format(Locale.getDefault(), "%.1f steps/min", cadence));
        }

        if (gaitSymmetryValue != null) {
            gaitSymmetryValue.setText(String.format(Locale.getDefault(), "%.1f%%", symmetry));
        }

        if (gaitVariabilityValue != null) {
            gaitVariabilityValue.setText(String.format(Locale.getDefault(), "%.1f ms", variability));
        }

        if (gaitStepLengthValue != null) {
            gaitStepLengthValue.setText(String.format(Locale.getDefault(), "%.2f m", stepLength));
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

        // Observe gait metrics
        viewModel.getGaitCadence().observe(getViewLifecycleOwner(), cadence -> {
            if (cadence != null && gaitCadenceValue != null) {
                gaitCadenceValue.setText(String.format(Locale.getDefault(), "%.1f steps/min", cadence));
            }
        });

        viewModel.getGaitSymmetry().observe(getViewLifecycleOwner(), symmetry -> {
            if (symmetry != null && gaitSymmetryValue != null) {
                gaitSymmetryValue.setText(String.format(Locale.getDefault(), "%.1f%%", symmetry));
            }
        });

        viewModel.getGaitVariability().observe(getViewLifecycleOwner(), variability -> {
            if (variability != null && gaitVariabilityValue != null) {
                gaitVariabilityValue.setText(String.format(Locale.getDefault(), "%.1f ms", variability));
            }
        });

        viewModel.getGaitStepLength().observe(getViewLifecycleOwner(), stepLength -> {
            if (stepLength != null && gaitStepLengthValue != null) {
                gaitStepLengthValue.setText(String.format(Locale.getDefault(), "%.2f m", stepLength));
            }
        });
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
        builder.setTitle("Understanding Gait Metrics");

        String message =
                "Cadence: Number of steps per minute. Normal range is 90-120 steps/min.\n\n" +
                        "Symmetry: How similar your left and right steps are. Lower percentage is better, with 0% being perfect symmetry.\n\n" +
                        "Variability: How consistent your step timing is. Lower values indicate more consistent steps.\n\n" +
                        "Step Length: Average length of each step. Typically 0.5-0.8m for adults.";

        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showGaitDetailDialog(GaitData gaitData) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Gait Analysis Details");

        // Create a formatted message with all the gait metrics
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        String date = dateFormat.format(new Date(gaitData.getTimestamp()));

        String message = "Date: " + date + "\n\n" +
                         "Status: " + gaitData.getStatus() + "\n\n" +
                         "Cadence: " + String.format(Locale.getDefault(), "%.1f steps/min", gaitData.getCadence()) + "\n" +
                         "Symmetry Index: " + String.format(Locale.getDefault(), "%.1f%%", gaitData.getSymmetryIndex()) + "\n" +
                         "Step Variability: " + String.format(Locale.getDefault(), "%.1f ms", gaitData.getStepVariability()) + "\n" +
                         "Step Length: " + String.format(Locale.getDefault(), "%.2f m", gaitData.getStepLength()) + "\n\n" +
                         "Interpretation:\n" + getGaitInterpretation(gaitData);

        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private String getGaitInterpretation(GaitData gaitData) {
        StringBuilder interpretation = new StringBuilder();

        // Cadence interpretation
        float cadence = gaitData.getCadence();
        if (cadence < 90) {
            interpretation.append("• Low cadence may indicate reduced mobility or fatigue.\n");
        } else if (cadence > 130) {
            interpretation.append("• High cadence may indicate shorter steps or rushing.\n");
        } else {
            interpretation.append("• Cadence is within normal range.\n");
        }

        // Symmetry interpretation
        double symmetry = gaitData.getSymmetryIndex();
        if (symmetry > 10) {
            interpretation.append("• Asymmetric gait may indicate favoring one side.\n");
        } else {
            interpretation.append("• Good symmetry between left and right steps.\n");
        }

        // Variability interpretation
        double variability = gaitData.getStepVariability();
        if (variability > 100) {
            interpretation.append("• High step variability may indicate inconsistent walking pattern.\n");
        } else {
            interpretation.append("• Consistent step timing indicates stable gait.\n");
        }

        return interpretation.toString();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register for gait analysis updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(GaitAnalysisService.ACTION_GAIT_ANALYSIS_COMPLETED);
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(gaitAnalysisReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister receiver to prevent leaks
        try {
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(gaitAnalysisReceiver);
        } catch (Exception e) {
            // Ignore if receiver wasn't registered
        }
    }

    /**
     * Shows a confirmation dialog before deleting gait history
     */
    private void showDeleteGaitHistoryConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Gait History");
        builder.setMessage("Are you sure you want to delete all gait analysis history? This action cannot be undone.");

        builder.setPositiveButton("Delete", (dialog, which) -> {
            // Call ViewModel method to delete gait history
            viewModel.deleteAllGaitData();
            Toast.makeText(requireContext(), "Gait history deleted", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
