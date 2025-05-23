package com.example.mobigait.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mobigait.MainActivity;
import com.example.mobigait.R;
import com.example.mobigait.model.Weight;
import com.example.mobigait.repository.WeightRepository;
import com.example.mobigait.utils.UserPreferences;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button backButton;
    private Button nextButton;
    private UserPreferences userPreferences;
    private OnboardingAdapter adapter;

    private String selectedGender = "";
    private float height = 0f;
    private float weight = 0f;
    private int age = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        userPreferences = new UserPreferences(this);

        // Initialize views
        viewPager = findViewById(R.id.onboardingViewPager);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);

        // Set up adapter
        adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);

        // Disable swiping
        viewPager.setUserInputEnabled(false);

        // Set up button listeners
        backButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();
            if (currentPosition > 0) {
                viewPager.setCurrentItem(currentPosition - 1);
            }
        });

        nextButton.setOnClickListener(v -> {
            int currentPosition = viewPager.getCurrentItem();

            // Validate current page
            if (validateCurrentPage(currentPosition)) {
                if (currentPosition < adapter.getItemCount() - 1) {
                    // Move to next page
                    viewPager.setCurrentItem(currentPosition + 1);
                } else {
                    // Save data and finish onboarding
                    saveUserData();
                    startMainActivity();
                }
            }
        });

        // Set up page change callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateButtonsVisibility(position);
            }
        });
    }

    private void updateButtonsVisibility(int position) {
        if (position == 0) {
            backButton.setVisibility(View.INVISIBLE);
        } else {
            backButton.setVisibility(View.VISIBLE);
        }

        if (position == adapter.getItemCount() - 1) {
            nextButton.setText("Finish");
        } else {
            nextButton.setText("Next");
        }
    }

    private boolean validateCurrentPage(int position) {
        View currentPage = adapter.getPageAtPosition(position);

        switch (position) {
            case 0: // Gender page
                RadioGroup genderRadioGroup = currentPage.findViewById(R.id.genderRadioGroup);
                int selectedId = genderRadioGroup.getCheckedRadioButtonId();

                if (selectedId == -1) {
                    Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
                    return false;
                }

                RadioButton radioButton = currentPage.findViewById(selectedId);
                selectedGender = radioButton.getText().toString();
                return true;

            case 1: // Height page
                EditText heightEditText = currentPage.findViewById(R.id.heightEditText);
                String heightStr = heightEditText.getText().toString();

                if (heightStr.isEmpty()) {
                    Toast.makeText(this, "Please enter your height", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    height = Float.parseFloat(heightStr);
                    if (height <= 0 || height > 250) {
                        Toast.makeText(this, "Please enter a valid height (1-250 cm)", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            case 2: // Weight page
                EditText weightEditText = currentPage.findViewById(R.id.weightEditText);
                String weightStr = weightEditText.getText().toString();

                if (weightStr.isEmpty()) {
                    Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    weight = Float.parseFloat(weightStr);
                    if (weight <= 0 || weight > 500) {
                        Toast.makeText(this, "Please enter a valid weight (1-500 kg)", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            case 3: // Age page
                EditText ageEditText = currentPage.findViewById(R.id.ageEditText);
                String ageStr = ageEditText.getText().toString();

                if (ageStr.isEmpty()) {
                    Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
                    return false;
                }

                try {
                    age = Integer.parseInt(ageStr);
                    if (age <= 0 || age > 120) {
                        Toast.makeText(this, "Please enter a valid age (1-120 years)", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;

            default:
                return true;
        }
    }

    private void saveUserData() {
        userPreferences.setGender(selectedGender);
        userPreferences.setHeight(height);
        userPreferences.setWeight(weight);
        userPreferences.setAge(age);
        userPreferences.setFirstTime(false);

        // Save initial weight to the weight database
        Weight initialWeight = new Weight(System.currentTimeMillis(), weight);
        WeightRepository weightRepository = new WeightRepository(getApplication());
        weightRepository.insert(initialWeight);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // ViewPager Adapter
    private class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

        private final List<Integer> layouts = new ArrayList<>();
        private final List<View> pages = new ArrayList<>();

        public OnboardingAdapter() {
            layouts.add(R.layout.onboarding_gender);
            layouts.add(R.layout.onboarding_height);
            layouts.add(R.layout.onboarding_weight);
            layouts.add(R.layout.onboarding_age);
        }

        @NonNull
        @Override
        public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(layouts.get(viewType), parent, false);
            pages.add(view);
            return new OnboardingViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
            // No binding needed as layouts are static
        }

        @Override
        public int getItemCount() {
            return layouts.size();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        public View getPageAtPosition(int position) {
            return pages.get(position);
        }

        class OnboardingViewHolder extends RecyclerView.ViewHolder {
            public OnboardingViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
