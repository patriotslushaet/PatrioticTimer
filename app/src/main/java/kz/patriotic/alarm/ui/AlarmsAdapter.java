package kz.patriotic.alarm.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.AlarmItem;

public class AlarmsAdapter extends RecyclerView.Adapter<AlarmsAdapter.AlarmViewHolder> {

    public interface OnAlarmListener {
        void onToggle(AlarmItem alarm, boolean enabled);
        void onDelete(AlarmItem alarm);
        void onEdit(AlarmItem alarm);
    }

    private final List<AlarmItem> alarms;
    private final OnAlarmListener listener;

    public AlarmsAdapter(List<AlarmItem> alarms, OnAlarmListener listener) {
        this.alarms = alarms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarm = alarms.get(position);
        holder.tvTime.setText(alarm.getFormattedTime());
        holder.tvLabel.setText(alarm.getLabel());
        holder.tvDays.setText(alarm.getDaysDisplayString());

        // Streak Eligibility Tag
        boolean isEligible = alarm.hasEligibleDays();
        if (isEligible) {
            holder.tvStreakBadge.setText(R.string.alarm_streak_eligible);
            holder.tvStreakBadge.setBackgroundResource(R.drawable.bg_pill_eligible);
            holder.tvStreakBadge.setTextColor(0xFF2ECC71);
        } else {
            holder.tvStreakBadge.setText(R.string.alarm_streak_ineligible);
            holder.tvStreakBadge.setBackgroundResource(R.drawable.bg_pill_ineligible);
            holder.tvStreakBadge.setTextColor(0xFFF39C12);
        }

        // Switch toggle
        holder.switchAlarm.setOnCheckedChangeListener(null);
        holder.switchAlarm.setChecked(alarm.isEnabled());
        holder.switchAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            alarm.setEnabled(isChecked);
            if (listener != null) listener.onToggle(alarm, isChecked);
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(alarm);
        });

        // Edit on card click
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(alarm);
        });
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvLabel, tvDays, tvStreakBadge;
        SwitchCompat switchAlarm;
        ImageView btnDelete;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_alarm_time);
            tvLabel = itemView.findViewById(R.id.tv_alarm_label);
            tvDays = itemView.findViewById(R.id.tv_alarm_days);
            tvStreakBadge = itemView.findViewById(R.id.tv_streak_badge);
            switchAlarm = itemView.findViewById(R.id.switch_alarm);
            btnDelete = itemView.findViewById(R.id.btn_delete_alarm);
        }
    }
}
