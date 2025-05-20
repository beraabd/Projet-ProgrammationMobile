package com.example.mobigait.utils;

import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static long[] getTodayTimeRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfDay = calendar.getTimeInMillis();

        return new long[]{startOfDay, endOfDay};
    }

    public static long[] getWeekTimeRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfWeek = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfWeek = calendar.getTimeInMillis();

        return new long[]{startOfWeek, endOfWeek};
    }

    public static long[] getMonthTimeRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long endOfMonth = calendar.getTimeInMillis();

        return new long[]{startOfMonth, endOfMonth};
    }

    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        minutes = minutes % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d h %02d min", hours, minutes);
        } else {
            return String.format(Locale.getDefault(), "%d min", minutes);
        }
    }



    public static String formatDate(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);

        return String.format("%d/%d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
    }
}
