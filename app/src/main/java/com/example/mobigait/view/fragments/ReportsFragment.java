package com.example.mobigait.view.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mobigait.R;
import com.example.mobigait.model.Step;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.viewmodel.ReportsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private ReportsViewModel viewModel;
    private BarChart barChart;
    private TextView totalStepsValue;
    private TextView averageStepsValue;
    private TextView goalMetValue;
    private ChipGroup timeRangeChipGroup;
    private ChipGroup metricChipGroup;
    private Chip weekChip;
    private Chip monthChip;
    private Chip stepsChip;
    private Chip distanceChip;
    private Chip caloriesChip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(ReportsViewModel.class);

        // Initialize views
        initializeViews(view);
        setupChipListeners();
        setupObservers();
        setupChart();
    }

    private void initializeViews(View view) {
        barChart = view.findViewById(R.id.barChart);
        totalStepsValue = view.findViewById(R.id.totalStepsValue);
        averageStepsValue = view.findViewById(R.id.averageStepsValue);
        goalMetValue = view.findViewById(R.id.goalMetValue);
        timeRangeChipGroup = view.findViewById(R.id.timeRangeChipGroup);
        metricChipGroup = view.findViewById(R.id.metricChipGroup);
        weekChip = view.findViewById(R.id.weekChip);
        monthChip = view.findViewById(R.id.monthChip);
        stepsChip = view.findViewById(R.id.stepsChip);
        distanceChip = view.findViewById(R.id.distanceChip);
        caloriesChip = view.findViewById(R.id.caloriesChip);
    }

    private void setupChipListeners() {
        // Time range chips
        // Time range chips
        weekChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setTimeRange(ReportsViewModel.TimeRange.WEEK);
            }
        });

        monthChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setTimeRange(ReportsViewModel.TimeRange.MONTH);
            }
        });

        // Metric chips
        stepsChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setMetric(ReportsViewModel.Metric.STEPS);
            }
        });

        distanceChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setMetric(ReportsViewModel.Metric.DISTANCE);
            }
        });

        caloriesChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setMetric(ReportsViewModel.Metric.CALORIES);
            }
        });
    }

    private void setupObservers() {
        // Observe step data changes
        viewModel.getStepData().observe(getViewLifecycleOwner(), this::updateChart);

        // Observe total steps
        viewModel.getTotalSteps().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                totalStepsValue.setText(String.format(Locale.getDefault(), "%,d", total));
            } else {
                totalStepsValue.setText("0");
            }
        });

        // Observe average steps
        viewModel.getAverageSteps().observe(getViewLifecycleOwner(), average -> {
            if (average != null) {
                averageStepsValue.setText(String.format(Locale.getDefault(), "%.0f", average));
            } else {
                averageStepsValue.setText("0");
            }
        });

        // Observe goal met days
        viewModel.getGoalMetDays().observe(getViewLifecycleOwner(), days -> {
            if (days != null) {
                int totalDays = viewModel.getSelectedTimeRange().getValue() == ReportsViewModel.TimeRange.WEEK ? 7 : 30;
                goalMetValue.setText(String.format(Locale.getDefault(), "%d/%d", days, totalDays));
            } else {
                goalMetValue.setText("0/7");
            }
        });

        // Observe metric changes to update chart
        viewModel.getSelectedMetric().observe(getViewLifecycleOwner(), metric -> {
            if (viewModel.getStepData().getValue() != null) {
                updateChart(viewModel.getStepData().getValue());
            }
        });

        // Observe time range changes to update chart labels
        viewModel.getSelectedTimeRange().observe(getViewLifecycleOwner(), timeRange -> {
            setupChart(); // Reset chart with new labels
        });
    }

    private void setupChart() {
        // Basic chart setup
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setHighlightFullBarEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.getLegend().setEnabled(false);

        // X-axis setup
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);

        // Set X-axis labels based on selected time range
        ReportsViewModel.TimeRange timeRange = viewModel.getSelectedTimeRange().getValue();
        if (timeRange == ReportsViewModel.TimeRange.WEEK) {
            String[] weekDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            xAxis.setValueFormatter(new IndexAxisValueFormatter(weekDays));
            xAxis.setLabelCount(7);
        } else {
            // For month view, we'll set labels in updateChart
            xAxis.setLabelCount(5, true); // Show fewer labels to avoid crowding
        }

        // Y-axis setup
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);

        // Empty data initially
        barChart.setData(new BarData());
        barChart.invalidate();
    }

    private void updateChart(List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            barChart.clear();
            barChart.invalidate();
            return;
        }

        ReportsViewModel.TimeRange timeRange = viewModel.getSelectedTimeRange().getValue();
        ReportsViewModel.Metric metric = viewModel.getSelectedMetric().getValue();
        int goalSteps = viewModel.getStepGoal();

        // Group data by day
        Map<Integer, Float> dataByDay = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthDayFormat = new SimpleDateFormat("d", Locale.getDefault());

        // Initialize all days with zero
        if (timeRange == ReportsViewModel.TimeRange.WEEK) {
            for (int i = 0; i < 7; i++) {
                dataByDay.put(i, 0f);
            }
        } else {
            calendar.setTime(new Date());
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= daysInMonth; i++) {
                dataByDay.put(i, 0f);
            }
        }

        // Aggregate data by day
        for (Step step : steps) {
            calendar.setTimeInMillis(step.getTimestamp());
            int day;

            if (timeRange == ReportsViewModel.TimeRange.WEEK) {
                day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday, 6 = Saturday
            } else {
                day = calendar.get(Calendar.DAY_OF_MONTH); // 1-31
            }

            float value = 0;
            switch (metric) {
                case STEPS:
                    value = step.getStepCount();
                    break;
                case DISTANCE:
                    value = (float) step.getDistance();
                    break;
                case CALORIES:
                    value = (float) step.getCalories();
                    break;
            }

            // Add to existing value for this day
            dataByDay.put(day, dataByDay.getOrDefault(day, 0f) + value);
        }

        // Create bar entries
        List<BarEntry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        if (timeRange == ReportsViewModel.TimeRange.WEEK) {
            for (int i = 0; i < 7; i++) {
                entries.add(new BarEntry(i, dataByDay.getOrDefault(i, 0f)));
            }
            // Week labels are set in setupChart
        } else {
            // For month view, we need day numbers as labels
            calendar.setTime(new Date());
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            for (int i = 1; i <= daysInMonth; i++) {
                entries.add(new BarEntry(i - 1, dataByDay.getOrDefault(i, 0f)));
                xLabels.add(String.valueOf(i));
            }

            // Set month day labels
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(xLabels));
            barChart.getXAxis().setLabelCount(5, true); // Show fewer labels to avoid crowding
        }

        // Create dataset
        BarDataSet dataSet = new BarDataSet(entries, "");

        // Set colors based on goal achievement
        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : entries) {
            if (metric == ReportsViewModel.Metric.STEPS && entry.getY() >= goalSteps) {
                colors.add(getResources().getColor(R.color.primary, null));
            } else {
                colors.add(getResources().getColor(R.color.red, null));
            }
        }
        dataSet.setColors(colors);

        // Format values based on metric
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0) return "";

                switch (metric) {
                    case STEPS:
                        return String.format(Locale.getDefault(), "%.0f", value);
                    case DISTANCE:
                        return String.format(Locale.getDefault(), "%.1f", value);
                    case CALORIES:
                        return String.format(Locale.getDefault(), "%.0f", value);
                    default:
                        return String.format(Locale.getDefault(), "%.0f", value);
                }
            }
        });

        // Adjust chart width based on number of entries
        int minWidth = getResources().getDisplayMetrics().widthPixels; // Default to screen width

        if (timeRange == ReportsViewModel.TimeRange.WEEK) {
            // For week view, default screen width is usually enough
            barChart.setMinimumWidth(minWidth);
        } else {
            // For month view, make sure each bar has enough space (at least 50dp)
            int daysInMonth = entries.size();
            int barWidthInDp = 50;
            int barWidthInPx = (int) (barWidthInDp * getResources().getDisplayMetrics().density);
            int requiredWidth = daysInMonth * barWidthInPx;

            // Set minimum width to either screen width or required width, whichever is larger
            barChart.setMinimumWidth(Math.max(minWidth, requiredWidth));
        }

        // Update chart
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f);
        barChart.setData(barData);

        // Update Y-axis label based on metric
        switch (metric) {
            case STEPS:
                barChart.getAxisLeft().setAxisMaximum(Math.max(goalSteps * 1.2f, dataSet.getYMax() * 1.1f));
                break;
            case DISTANCE:
            case CALORIES:
                barChart.getAxisLeft().setAxisMaximum(dataSet.getYMax() * 1.1f);
                break;
        }

        barChart.invalidate();
    }
}
