package com.gratus.retrack;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<RelapseLog> historyList;

    public HistoryAdapter(List<RelapseLog> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_historical_records, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RelapseLog item = historyList.get(position);

        // 1. Relapse Number (Count backwards so newest is highest number)
        int relapseNumber = historyList.size() - position;
        holder.tvTitle.setText("Reset #" + relapseNumber);

        // 2. Date Formatting (e.g., 12/12/2025 • 1700hrs)
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(item.endTime);
        String datePart = DateFormat.format("dd/MM/yyyy", cal).toString();
        String timePart = DateFormat.format("HH:mm", cal).toString();
        holder.tvTimestamp.setText(datePart + " • " + timePart + "hrs");

        // 3. Texts
        holder.tvReason.setText(item.reason.isEmpty() ? "No reason recorded" : item.reason);
        holder.tvNextStepsDesc.setText(item.nextSteps.isEmpty() ? "No steps recorded" : item.nextSteps);

        // 4. Duration Formatting
        long days = TimeUnit.MILLISECONDS.toDays(item.duration);
        long hours = TimeUnit.MILLISECONDS.toHours(item.duration) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(item.duration) % 60;

        holder.tvDuration.setText(String.format(Locale.getDefault(),
                "Streak lasted: %dd %dh %dm", days, hours, minutes));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTimestamp, tvReason, tvNextStepsDesc, tvDuration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.relapse_count);
            tvTimestamp = itemView.findViewById(R.id.timestamp);
            tvReason = itemView.findViewById(R.id.reason);
            tvNextStepsDesc = itemView.findViewById(R.id.next_steps_desc);
            tvDuration = itemView.findViewById(R.id.streak_duration);
        }
    }

}