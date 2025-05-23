package com.example.mobigait.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobigait.R;
import com.example.mobigait.model.GaitData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GaitHistoryAdapter extends ListAdapter<GaitData, GaitHistoryAdapter.GaitViewHolder> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public GaitHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<GaitData> DIFF_CALLBACK = new DiffUtil.ItemCallback<GaitData>() {
        @Override
        public boolean areItemsTheSame(@NonNull GaitData oldItem, @NonNull GaitData newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp();
        }

        @Override
        public boolean areContentsTheSame(@NonNull GaitData oldItem, @NonNull GaitData newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getCadence() == newItem.getCadence() &&
                   oldItem.getStepVariability() == newItem.getStepVariability() &&
                   oldItem.getSymmetryIndex() == newItem.getSymmetryIndex() &&
                   oldItem.getStepLength() == newItem.getStepLength();
        }
    };

    @NonNull
    @Override
    public GaitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gait_history, parent, false);
        return new GaitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GaitViewHolder holder, int position) {
        GaitData gaitData = getItem(position);
        holder.bind(gaitData);
    }

    static class GaitViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView statusText;
        private final TextView detailsText;

        public GaitViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.gaitHistoryDate);
            statusText = itemView.findViewById(R.id.gaitHistoryStatus);
            detailsText = itemView.findViewById(R.id.gaitHistoryDetails);
        }

        public void bind(GaitData gaitData) {
            // Format date
            String formattedDate = DATE_FORMAT.format(new Date(gaitData.getTimestamp()));
            dateText.setText(formattedDate);

            // Set status with appropriate color
            String status = gaitData.getStatus();
            statusText.setText(status);

            if (status.equals("Normal")) {
                statusText.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                statusText.setTextColor(Color.parseColor("#F44336")); // Red
            }

            // Set details
            String details = String.format(Locale.getDefault(),
                    "Cadence: %.1f steps/min • Symmetry: %.1f%% • Variability: %.1f ms",
                    gaitData.getCadence(), gaitData.getSymmetryIndex(), gaitData.getStepVariability());
            detailsText.setText(details);
        }
    }
}