package com.example.mobigait.view.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.model.Weight;
import com.example.mobigait.viewmodel.HealthViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthFragment extends Fragment {

    private HealthViewModel viewModel;

    // UI elements
    private TextView gaitStatusValue;
    private TextView gaitStatusDescription;
    private TextView bmiValue;
    private TextView bmiCategory;
    private TextView bmiDescription;
    private View bmiIndicator;
    private LineChart weightChart;
    private FloatingActionButton addWeightButton;

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
        setupListeners();
        setupObservers();
        setupWeightChart();
    }

    private void initializeViews(View view) {
        gaitStatusValue = view.findViewById(R.id.gaitStatusValue);
        gaitStatusDescription = view.findViewById(R.id.gaitStatusDescription);
        bmiValue = view.findViewById(R.id.bmiValue);
        bmiCategory = view.findViewById(R.id.bmiCategory);
        bmiDescription = view.findViewById(R.id.bmiDescription);
        bmiIndicator = view.findViewById(R.id.bmiIndicator);
        weightChart = view.findViewById(R.id.weightChart);
        addWeightButton = view.findViewById(R.id.addWeightButton);
    }

    private void setupListeners() {
        addWeightButton.setOnClickListener(v -> showAddWeightDialog());
    }

    private void setupObservers() {
        // Observe gait status
        viewModel.getGaitStatus().observe(getViewLifecycleOwner(), status -> {
            if ("Normal".equals(status)) {
                gaitStatusValue.setText("Normal ✅");
                gaitStatusValue.setTextColor(getResources().getColor(R.color.primary, null));
                gaitStatusDescription.setText("Your walking pattern appears to be normal based on sensor data analysis.");
            } else {
                gaitStatusValue.setText("Abnormal ❗");
                gaitStatusValue.setTextColor(getResources().getColor(R.color.red, null));
                gaitStatusDescription.setText("Your walking pattern shows some irregularities. Consider consulting a healthcare professional.");
            }
        });

        // Observe BMI
        viewModel.getCurrentBmi().observe(getViewLifecycleOwner(), bmi -> {
            if (bmi != null) {
                bmiValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                updateBmiIndicator(bmi);
            }
        });

        // Observe BMI category
        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category -> {
            bmiCategory.setText(category);
            updateBmiDescription(category);
        });

        // Observe weight history for chart
        viewModel.getWeightHistory().observe(getViewLifecycleOwner(), this::updateWeightChart);
    }

    private void updateBmiIndicator(float bmi) {
        // Position the indicator based on BMI value
        FrameLayout container = (FrameLayout) bmiIndicator.getParent();
        float containerWidth = container.getWidth();

        if (containerWidth > 0) {
            float position;

            if (bmi < 16) {
                position = 0.1f * containerWidth;
            } else if (bmi > 35) {
                position = 0.9f * containerWidth;
            } else {
                // Map BMI range 16-35 to position 0.1-0.9
                position = (0.1f + (bmi - 16) * 0.8f / 19) * containerWidth;
            }

            bmiIndicator.setX(position - bmiIndicator.getWidth() / 2f);
        }
    }

    private void updateBmiDescription(String category) {
        switch (category) {
            case "Underweight":
                bmiDescription.setText("Your BMI indicates you are underweight. Consider consulting a healthcare professional about healthy weight gain strategies.");
                break;
            case "Normal":
                bmiDescription.setText("Your BMI is within the normal range. Maintaining a healthy weight can help prevent various health issues.");
                break;
            case "Overweight":
                bmiDescription.setText("Your BMI indicates you are overweight. Consider adopting healthier eating habits and increasing physical activity.");
                break;
            case "Obese":
                bmiDescription.setText("Your BMI indicates obesity. This increases your risk for various health conditions. Consider consulting a healthcare professional.");
                break;
            case "Severely Obese":
                bmiDescription.setText("Your BMI indicates severe obesity. This significantly increases your risk for serious health conditions. Please consult a healthcare professional.");
                break;
        }
    }

    private void setupWeightChart() {
        // Basic chart setup
        weightChart.getDescription().setEnabled(false);
        weightChart.setDrawGridBackground(false);
        weightChart.setTouchEnabled(true);
        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setPinchZoom(true);
        weightChart.getLegend().setEnabled(false);
        weightChart.setNoDataText("No weight data available");
        weightChart.setNoDataTextColor(Color.BLACK);

        // X-axis setup
        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                // Convert timestamp to date string
                return dateFormat.format(new Date((long) value));
            }
        });

        // Y-axis setup
        weightChart.getAxisLeft().setDrawGridLines(true);
        weightChart.getAxisLeft().setAxisMinimum(0f); // Start from 0
        weightChart.getAxisRight().setEnabled(false);

        // Empty data initially
        weightChart.setData(new LineData());
        weightChart.invalidate();
    }

    private void updateWeightChart(List<Weight> weights) {
        if (weights == null || weights.isEmpty()) {
            weightChart.clear();
            weightChart.invalidate();
            return;
        }

        List<Entry> entries = new ArrayList<>();

        // Convert weight data to entries
        for (Weight weight : weights) {
            entries.add(new Entry(weight.getTimestamp(), weight.getWeight()));
        }

        // Create dataset
        LineDataSet dataSet = new LineDataSet(entries, "Weight");
        dataSet.setColor(getResources().getColor(R.color.primary, null));
        dataSet.setCircleColor(getResources().getColor(R.color.primary, null));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f kg", value);
            }
        });
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getResources().getColor(R.color.primary_light, null));
        dataSet.setFillAlpha(100);

        // Set data to chart
        LineData lineData = new LineData(dataSet);
        weightChart.setData(lineData);

        // Calculate Y-axis range based on weight values
        float minWeight = Float.MAX_VALUE;
        float maxWeight = 0;
        for (Weight weight : weights) {
            if (weight.getWeight() < minWeight) minWeight = weight.getWeight();
            if (weight.getWeight() > maxWeight) maxWeight = weight.getWeight();
        }

        // Add padding to Y-axis range (10% of the weight range)
        float range = maxWeight - minWeight;
        float padding = Math.max(range * 0.1f, 5f); // At least 5kg padding or 10% of range

        // Set Y-axis limits with padding
        weightChart.getAxisLeft().setAxisMinimum(Math.max(0, minWeight - padding));
        weightChart.getAxisLeft().setAxisMaximum(maxWeight + padding);

        // Refresh chart
        weightChart.notifyDataSetChanged();
        weightChart.invalidate();

        // Ensure X-axis shows dates properly
        weightChart.getXAxis().setLabelCount(Math.min(weights.size(), 5), true);
    }

    private void showAddWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Weight");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Enter weight in kg");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float weight = Float.parseFloat(input.getText().toString());
                if (weight > 0 && weight < 500) {
                    viewModel.addWeight(weight);
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid weight (1-500 kg)", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}
