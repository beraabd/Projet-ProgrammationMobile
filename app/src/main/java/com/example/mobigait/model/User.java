package com.example.mobigait.model;

public class User {
    private String gender;
    private float height; // in cm
    private float weight; // in kg
    private int age;
    private int dailyStepGoal;

    public User(String gender, float height, float weight, int age) {
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.age = age;
        this.dailyStepGoal = 10000; // Default goal
    }

    // Getters and setters
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getDailyStepGoal() {
        return dailyStepGoal;
    }

    public void setDailyStepGoal(int dailyStepGoal) {
        this.dailyStepGoal = dailyStepGoal;
    }

    // Calculate BMI
    public float calculateBMI() {
        if (height <= 0) return 0;
        return weight / ((height/100) * (height/100));
    }
}
