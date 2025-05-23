package com.example.mobigait.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.example.mobigait.model.Step;
import com.example.mobigait.repository.StepRepository;
import com.example.mobigait.utils.DateUtils;
import com.example.mobigait.utils.UserPreferences;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ReportsViewModel extends AndroidViewModel {
    private static final String TAG = "ReportsViewModel";

    private final StepRepository repository;
    private final UserPreferences userPreferences;

    // Time range selection
    private final MutableLiveData<TimeRange> selectedTimeRange = new MutableLiveData<>(TimeRange.WEEK);

    // Metric selection
    private final MutableLiveData<Metric> selectedMetric = new MutableLiveData<>(Metric.STEPS);

    // Data
    private final LiveData<List<Step>> stepData;
    private final LiveData<Integer> totalSteps;
    private final LiveData<Double> averageSteps;
    private final MutableLiveData<Integer> goalMetDays = new MutableLiveData<>(0);

    public enum TimeRange {
        WEEK,
        MONTH
    }

    public enum Metric {
        STEPS,
        DISTANCE,
        CALORIES
    }

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        repository = new StepRepository(application);
        userPreferences = new UserPreferences(application);

        // Transform time range selection to actual data
        stepData = Transformations.switchMap(selectedTimeRange, timeRange -> {
            long[] range = getTimeRange(timeRange);
            return repository.getStepsBetweenDates(range[0], range[1]);
        });

        // Calculate total steps based on selected time range
        totalSteps = Transformations.switchMap(selectedTimeRange, timeRange -> {
            long[] range = getTimeRange(timeRange);
            return repository.getTotalStepsBetweenDates(range[0], range[1]);
        });

        // Calculate average steps based on selected time range
        averageSteps = Transformations.switchMap(selectedTimeRange, timeRange -> {
            long[] range = getTimeRange(timeRange);
            return repository.getAverageStepsBetweenDates(range[0], range[1]);
        });

        // Observe step data to calculate goal met days
        stepData.observeForever(steps -> {
            if (steps != null) {
                calculateGoalMetDays(steps);
            }
        });
    }

    private long[] getTimeRange(TimeRange timeRange) {
        switch (timeRange) {
            case WEEK:
                return DateUtils.getWeekTimeRange();
            case MONTH:
                return DateUtils.getMonthTimeRange();
            default:
                return DateUtils.getWeekTimeRange();
        }
    }

    private void calculateGoalMetDays(List<Step> steps) {
        int goalSteps = userPreferences.getStepGoal();
        int daysMetGoal = 0;

        // Group steps by day and check if goal was met
        Calendar calendar = Calendar.getInstance();

        // For week view, we need to check each day
        if (selectedTimeRange.getValue() == TimeRange.WEEK) {
            boolean[] dayMetGoal = new boolean[7]; // Sunday to Saturday

            for (Step step : steps) {
                calendar.setTimeInMillis(step.getTimestamp());
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // 0 = Sunday, 6 = Saturday

                if (step.getStepCount() >= goalSteps) {
                    dayMetGoal[dayOfWeek] = true;
                }
            }

            // Count days where goal was met
            for (boolean metGoal : dayMetGoal) {
                if (metGoal) daysMetGoal++;
            }

            goalMetDays.postValue(daysMetGoal);
        }
        // For month view, we need to check each day of the month
        else if (selectedTimeRange.getValue() == TimeRange.MONTH) {
            calendar.setTimeInMillis(System.currentTimeMillis());
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            boolean[] dayMetGoal = new boolean[daysInMonth];

            for (Step step : steps) {
                calendar.setTimeInMillis(step.getTimestamp());
                int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH) - 1; // 0-based index

                if (step.getStepCount() >= goalSteps) {
                    dayMetGoal[dayOfMonth] = true;
                }
            }

            // Count days where goal was met
            for (boolean metGoal : dayMetGoal) {
                if (metGoal) daysMetGoal++;
            }

            goalMetDays.postValue(daysMetGoal);
        }
    }

    // Setters for user selections
    public void setTimeRange(TimeRange timeRange) {
        selectedTimeRange.setValue(timeRange);
    }

    public void setMetric(Metric metric) {
        selectedMetric.setValue(metric);
    }

    // Getters for LiveData
    public LiveData<TimeRange> getSelectedTimeRange() {
        return selectedTimeRange;
    }

    public LiveData<Metric> getSelectedMetric() {
        return selectedMetric;
    }

    public LiveData<List<Step>> getStepData() {
        return stepData;
    }

    public LiveData<Integer> getTotalSteps() {
        return totalSteps;
    }

    public LiveData<Double> getAverageSteps() {
        return averageSteps;
    }

    public LiveData<Integer> getGoalMetDays() {
        return goalMetDays;
    }

    public int getStepGoal() {
        return userPreferences.getStepGoal();
    }

    /**
     * Calculate and return the average steps per day for the current week
     */
    public LiveData<Double> getWeeklyAverageSteps() {
        MutableLiveData<Double> weeklyAverage = new MutableLiveData<>();

        // Get the time range for the current week
        long[] weekTimeRange = DateUtils.getWeekTimeRange();
        long startOfWeek = weekTimeRange[0];
        long endOfWeek = weekTimeRange[1];

        // Get the average steps between these dates
        LiveData<Double> averageSteps = repository.getAverageStepsBetweenDates(startOfWeek, endOfWeek);

        // Observe the average steps and update the weekly average
        averageSteps.observeForever(new Observer<Double>() {
            @Override
            public void onChanged(Double average) {
                if (average != null) {
                    weeklyAverage.setValue(average);
                } else {
                    weeklyAverage.setValue(0.0);
                }

                // Remove the observer to prevent memory leaks
                averageSteps.removeObserver(this);
            }
        });

        return weeklyAverage;
    }
}
