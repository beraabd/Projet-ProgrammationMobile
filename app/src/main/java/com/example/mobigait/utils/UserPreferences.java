package com.example.mobigait.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPreferences {
    private static final String PREF_NAME = "user_preferences";
    private static final String KEY_FIRST_TIME = "first_time";
    private static final String KEY_GENDER = "gender";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_WEIGHT = "weight";
    private static final String KEY_AGE = "age";
    private static final String KEY_STEP_GOAL = "step_goal";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SENSOR_SENSITIVITY = "sensor_sensitivity";
    private static final String KEY_SENSOR_THRESHOLD = "sensor_threshold";
    private static final String KEY_STEP_COUNT = "step_count";

    private final SharedPreferences preferences;

    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFirstTime() {
        return preferences.getBoolean(KEY_FIRST_TIME, true);
    }

    public void setFirstTime(boolean firstTime) {
        preferences.edit().putBoolean(KEY_FIRST_TIME, firstTime).apply();
    }

    public String getGender() {
        return preferences.getString(KEY_GENDER, "Male");
    }

    public void setGender(String gender) {
        preferences.edit().putString(KEY_GENDER, gender).apply();
    }

    public float getHeight() {
        return preferences.getFloat(KEY_HEIGHT, 170f);
    }

    public void setHeight(float height) {
        preferences.edit().putFloat(KEY_HEIGHT, height).apply();
    }

    public float getWeight() {
        return preferences.getFloat(KEY_WEIGHT, 70f);
    }

    public void setWeight(float weight) {
        preferences.edit().putFloat(KEY_WEIGHT, weight).apply();
    }

    public int getAge() {
        return preferences.getInt(KEY_AGE, 30);
    }

    public void setAge(int age) {
        preferences.edit().putInt(KEY_AGE, age).apply();
    }

    public int getStepGoal() {
        return preferences.getInt(KEY_STEP_GOAL, 10000); // Default goal is 10,000 steps
    }

    public void setStepGoal(int stepGoal) {
        preferences.edit().putInt(KEY_STEP_GOAL, stepGoal).apply();
    }

    public String getTheme() {
        return preferences.getString(KEY_THEME, "Light");
    }

    public void setTheme(String theme) {
        preferences.edit().putString(KEY_THEME, theme).apply();
    }

    public String getSensorSensitivity() {
        return preferences.getString(KEY_SENSOR_SENSITIVITY, "Medium");
    }

    public void setSensorSensitivity(String sensitivity) {
        preferences.edit().putString(KEY_SENSOR_SENSITIVITY, sensitivity).apply();
    }

    public float getSensorThreshold() {
        return preferences.getFloat(KEY_SENSOR_THRESHOLD, 12.0f); // Default medium sensitivity
    }

    public void setSensorThreshold(float threshold) {
        preferences.edit().putFloat(KEY_SENSOR_THRESHOLD, threshold).apply();
    }

    public int getStepCount() {
        return preferences.getInt(KEY_STEP_COUNT, 0);
    }

    public void setStepCount(int stepCount) {
        preferences.edit().putInt(KEY_STEP_COUNT, stepCount).apply();
    }

    public void resetStepCount() {
        preferences.edit().putInt(KEY_STEP_COUNT, 0).apply();
    }

    public boolean hasCompletedOnboarding() {
        return !getGender().isEmpty() && getHeight() > 0 && getWeight() > 0 && getAge() > 0;
    }
}

