package com.example.mobigait.view.fragments;

import android.content.Context;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.model.Weight;
import com.example.mobigait.utils.UserPreferences;
import com.example.mobigait.viewmodel.HealthViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.highlight.Highlight;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HealthFragment extends Fragment {

    private HealthViewModel viewModel;
    private UserPreferences userPreferences;

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
        userPreferences = new UserPreferences(requireContext());

        // Initialize views
        initializeViews(view);
        setupClickListeners();
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

    private void setupClickListeners() {
        addWeightButton.setOnClickListener(v -> showAddWeightDialog());
    }

    private void setupObservers() {
        // Observe BMI
        viewModel.getCurrentBmi().observe(getViewLifecycleOwner(), bmi -> {
            if (bmi != null) {
                bmiValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));
                updateBmiIndicator(bmi);
            }
        });

        // Observe BMI category
        viewModel.getBmiCategory().observe(getViewLifecycleOwner(), category -> {
            if (category != null) {
                bmiCategory.setText(category);
                updateBmiDescription(category);

                // Change BMI value color based on category
                int color;
                switch (category) {
                    case "Underweight":
                        color = getResources().getColor(R.color.blue, null);
                        break;
                    case "Normal":
                        color = getResources().getColor(R.color.green, null);
                        break;
                    case "Overweight":
                        color = getResources().getColor(R.color.yellow, null);
                        break;
                    case "Obese":
                        color = getResources().getColor(R.color.orange, null);
                        break;
                    case "Severely Obese":
                        color = getResources().getColor(R.color.red, null);
                        break;
                    default:
                        color = getResources().getColor(R.color.primary, null);
                        break;
                }

                // Apply the color to the BMI value text
                bmiValue.setTextColor(color);
                bmiCategory.setTextColor(color);
            }
        });

        // Observe gait status
        viewModel.getGaitStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                if (status.equals("Normal")) {
                    gaitStatusValue.setText("Normal ✅");
                    gaitStatusValue.setTextColor(getResources().getColor(R.color.primary, null));
                    gaitStatusDescription.setText("Your walking pattern appears to be normal based on sensor data analysis.");
                } else {
                    gaitStatusValue.setText(status);
                    gaitStatusValue.setTextColor(getResources().getColor(R.color.red, null));
                    gaitStatusDescription.setText("Not enough data to analyze your walking pattern. Please walk more to get an accurate assessment.");
                }
            }
        });

        // Observe weight history for chart
        viewModel.getWeightHistory().observe(getViewLifecycleOwner(), this::updateWeightChart);

        // Observe current weight
        viewModel.getCurrentWeight().observe(getViewLifecycleOwner(), weight -> {
            // This will trigger BMI calculation in the ViewModel
        });
    }

    private void updateBmiIndicator(float bmi) {
        // Calculate position on the BMI scale (width percentage)
        float position;

        if (bmi < 18.5) {
            // Underweight: 0-20%
            position = (bmi / 18.5f) * 0.2f;
        } else if (bmi < 25) {
            // Normal: 20-40%
            position = 0.2f + ((bmi - 18.5f) / 6.5f) * 0.2f;
        } else if (bmi < 30) {
            // Overweight: 40-60%
            position = 0.4f + ((bmi - 25f) / 5f) * 0.2f;
        } else if (bmi < 35) {
            // Obese: 60-80%
            position = 0.6f + ((bmi - 30f) / 5f) * 0.2f;
        } else {
            // Severely Obese: 80-100%
            position = Math.min(0.8f + ((bmi - 35f) / 15f) * 0.2f, 1.0f);
        }

        // Get the width of the container
        ViewGroup container = (ViewGroup) bmiIndicator.getParent();
        int containerWidth = container.getWidth();

        // Set the position of the indicator
        if (containerWidth > 0) {
            int indicatorPosition = (int) (containerWidth * position) - (bmiIndicator.getWidth() / 2);
            bmiIndicator.setX(indicatorPosition);
        }
    }

    private void updateBmiDescription(String category) {
        switch (category) {
            case "Underweight":
                bmiDescription.setText("Your BMI indicates you are underweight. Consider consulting with a healthcare provider about healthy weight gain strategies.");
                break;
            case "Normal":
                bmiDescription.setText("Your BMI is within the normal range. Maintaining a healthy weight can help prevent various health issues.");
                break;
            case "Overweight":
                bmiDescription.setText("Your BMI indicates you are overweight. Consider incorporating more physical activity and a balanced diet.");
                break;
            case "Obese":
                bmiDescription.setText("Your BMI indicates obesity. This increases risk for health conditions. Consider consulting a healthcare provider.");
                break;
            case "Severely Obese":
                bmiDescription.setText("Your BMI indicates severe obesity. This significantly increases health risks. Please consult with a healthcare provider.");
                break;
            default:
                bmiDescription.setText("BMI is a screening tool but not a diagnostic of body fatness or health.");
                break;
        }
    }

    private void setupWeightChart() {
        // Basic chart setup
        weightChart.getDescription().setEnabled(false);
        weightChart.setDrawGridBackground(false);
        weightChart.setBackgroundColor(getResources().getColor(R.color.white, null));

        // Enable interaction
        weightChart.setTouchEnabled(true);
        weightChart.setDragEnabled(true);
        weightChart.setScaleEnabled(true);
        weightChart.setPinchZoom(true);
        weightChart.setDoubleTapToZoomEnabled(true);

        // Set visible range
        weightChart.setVisibleXRangeMaximum(7); // Show 7 days at a time by default
        weightChart.setExtraOffsets(10f, 10f, 10f, 10f); // Add padding

        // Customize legend
        weightChart.getLegend().setEnabled(false);

        // X-axis setup
        XAxis xAxis = weightChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(getResources().getColor(R.color.primary, null));
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                // Convert timestamp to date
                return dateFormat.format(new Date((long) value));
            }
        });

        // Y-axis setup
        weightChart.getAxisLeft().setDrawGridLines(true);
        weightChart.getAxisLeft().setGridColor(getResources().getColor(R.color.light_gray, null));
        weightChart.getAxisLeft().setTextColor(getResources().getColor(R.color.primary, null));
        weightChart.getAxisLeft().setTextSize(10f);
        weightChart.getAxisLeft().setAxisMinimum(0f); // Start from 0

        // Disable right Y-axis
        weightChart.getAxisRight().setEnabled(false);

        // Add empty state message
        weightChart.setNoDataText("No weight data available");
        weightChart.setNoDataTextColor(getResources().getColor(R.color.primary, null));

        // Add marker view for better data point display
        WeightMarkerView markerView = new WeightMarkerView(requireContext());
        markerView.setChartView(weightChart);
        weightChart.setMarker(markerView);
    }

    private class WeightMarkerView extends com.github.mikephil.charting.components.MarkerView {
        private final TextView tvContent;

        public WeightMarkerView(Context context) {
            super(context, R.layout.weight_marker_view);
            tvContent = findViewById(R.id.tvContent);
        }

        @Override
        public void refreshContent(Entry e, com.github.mikephil.charting.highlight.Highlight highlight) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String date = dateFormat.format(new Date((long) e.getX()));
            tvContent.setText(String.format(Locale.getDefault(), "%s\n%.1f kg", date, e.getY()));
            super.refreshContent(e, highlight);
        }

        @Override
        public com.github.mikephil.charting.utils.MPPointF getOffset() {
            return new com.github.mikephil.charting.utils.MPPointF(-(getWidth() / 2f), -getHeight());
        }
    }

    private void updateWeightChart(List<Weight> weights) {
        if (weights == null || weights.isEmpty()) {
            weightChart.setNoDataText("No weight data available");
            weightChart.invalidate();
            return;
        }

        // Sort weights by timestamp
        List<Weight> sortedWeights = new ArrayList<>(weights);
        Collections.sort(sortedWeights, (w1, w2) -> Long.compare(w1.getTimestamp(), w2.getTimestamp()));

        List<Entry> entries = new ArrayList<>();
        for (Weight weight : sortedWeights) {
            entries.add(new Entry(weight.getTimestamp(), weight.getWeight()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weight");

        // Customize the line
        dataSet.setColor(getResources().getColor(R.color.primary, null));
        dataSet.setLineWidth(2f);

        // Customize the circles
        dataSet.setCircleColor(getResources().getColor(R.color.primary, null));
        dataSet.setCircleHoleColor(getResources().getColor(R.color.white, null));
        dataSet.setCircleRadius(4f);

        // Customize the values
        dataSet.setDrawValues(false); // Don't show values on points, use marker instead

        // Add gradient fill
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(getResources().getDrawable(R.drawable.chart_gradient_fill, null));

        // Add animation
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve

        LineData lineData = new LineData(dataSet);
        weightChart.setData(lineData);

        // Calculate Y-axis range based on data
        float minWeight = Float.MAX_VALUE;
        float maxWeight = Float.MIN_VALUE;
        for (Entry entry : entries) {
            minWeight = Math.min(minWeight, entry.getY());
            maxWeight = Math.max(maxWeight, entry.getY());
        }

        // Set Y-axis range with some padding
        float padding = (maxWeight - minWeight) * 0.2f;
        weightChart.getAxisLeft().setAxisMinimum(Math.max(0, minWeight - padding));
        weightChart.getAxisLeft().setAxisMaximum(maxWeight + padding);

        // Animate the chart
        weightChart.animateX(1000);

        // Move to the latest entry
        if (!entries.isEmpty()) {
            weightChart.moveViewToX(entries.get(entries.size() - 1).getX());
        }

        weightChart.invalidate();
    }

    private void showAddWeightDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add Weight");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setHint("Weight in kg");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            try {
                float weight = Float.parseFloat(input.getText().toString());
                if (weight > 0 && weight < 500) {
                    viewModel.addWeight(weight);
                    Toast.makeText(requireContext(), "Weight saved", Toast.LENGTH_SHORT).show();

                    // Also update in user preferences
                    userPreferences.setWeight(weight);
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
}
