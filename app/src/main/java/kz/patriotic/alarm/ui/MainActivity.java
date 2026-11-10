package kz.patriotic.alarm.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Arrays;
import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.AlarmItem;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.util.AlarmStorage;
import kz.patriotic.alarm.util.StreakManager;

public class MainActivity extends AppCompatActivity implements AlarmsAdapter.OnAlarmListener {

    private static final int REQ_PERMISSIONS_CAMERA = 101;
    private static final int REQ_OVERLAY_PERMISSION = 102;

    private TextView tvStreakNumber, tvStreakMisses, tvTierName, tvTierDesc;
    private View previewWidgetRoot;
    private TextView widgetStreakCount, widgetTierTitle, widgetMisses, widgetNextAlarm, widgetStatusBadge;
    private ImageView widgetFlag;
    private RecyclerView recyclerAlarms;
    private AlarmsAdapter alarmsAdapter;
    private List<AlarmItem> alarmList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupAlarmsRecycler();
        updateStreakUI();

        // Check required permissions on startup
        checkAndPromptStartupPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStreakUI();
        if (alarmsAdapter != null) {
            alarmList.clear();
            alarmList.addAll(AlarmStorage.getAlarms(this));
            alarmsAdapter.notifyDataSetChanged();
        }
    }

    private void initViews() {
        tvStreakNumber = findViewById(R.id.tv_streak_number);
        tvStreakMisses = findViewById(R.id.tv_streak_misses);
        tvTierName = findViewById(R.id.tv_tier_name);
        tvTierDesc = findViewById(R.id.tv_tier_desc);

        // In-App Widget Preview references
        previewWidgetRoot = findViewById(R.id.preview_widget);
        widgetStreakCount = previewWidgetRoot.findViewById(R.id.widget_streak_count);
        widgetTierTitle = previewWidgetRoot.findViewById(R.id.widget_tier_title);
        widgetMisses = previewWidgetRoot.findViewById(R.id.widget_misses);
        widgetNextAlarm = previewWidgetRoot.findViewById(R.id.widget_next_alarm);
        widgetStatusBadge = previewWidgetRoot.findViewById(R.id.widget_status_badge);
        widgetFlag = previewWidgetRoot.findViewById(R.id.widget_flag);

        recyclerAlarms = findViewById(R.id.recycler_alarms);

        findViewById(R.id.btn_tutorial).setOnClickListener(v -> showTutorialDialog());
        findViewById(R.id.btn_view_roadmap).setOnClickListener(v -> showRoadmapDialog());

        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fab_add_alarm);
        fabAdd.setOnClickListener(v -> showEditAlarmDialog(null));

        findViewById(R.id.btn_add_alarm_header).setOnClickListener(v -> showEditAlarmDialog(null));
    }

    private void setupAlarmsRecycler() {
        alarmList = AlarmStorage.getAlarms(this);
        alarmsAdapter = new AlarmsAdapter(alarmList, this);
        recyclerAlarms.setLayoutManager(new LinearLayoutManager(this));
        recyclerAlarms.setAdapter(alarmsAdapter);
    }

    private void updateStreakUI() {
        int streak = StreakManager.getCurrentStreak(this);
        StreakTier tier = StreakManager.getCurrentTier(this);
        int missesRemaining = StreakManager.getMonthlyMissesRemaining(this);

        tvStreakNumber.setText(String.valueOf(streak));
        tvStreakMisses.setText(missesRemaining + " из 2 запас");
        tvTierName.setText("Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ")");

        try {
            int accent = Color.parseColor(tier.getAccentColorHex());
            tvTierName.setTextColor(accent);
        } catch (Exception ignored) {}

        int nextTierDays = tier.getMaxDays() + 1;
        int daysLeft = Math.max(1, nextTierDays - streak);
        tvTierDesc.setText(tier.getDescription() + " • До след. ранга: " + daysLeft + " дн.");

        // Update In-App Widget Preview
        if (widgetFlag != null) {
            widgetFlag.setImageResource(R.drawable.flag_kz);
        }
        widgetStreakCount.setText(streak + " ДНЕЙ");
        widgetTierTitle.setText("Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ")");
        try {
            widgetTierTitle.setTextColor(Color.parseColor(tier.getAccentColorHex()));
        } catch (Exception ignored) {}

        widgetMisses.setText(missesRemaining + "/2 запас");

        String nextAlarmStr = "Будильник не установлен";
        boolean isEligible = false;
        for (AlarmItem a : alarmList) {
            if (a.isEnabled()) {
                nextAlarmStr = "Будильник: " + a.getFormattedTime();
                isEligible = a.hasEligibleDays();
                break;
            }
        }
        widgetNextAlarm.setText(nextAlarmStr);
        widgetStatusBadge.setText(isEligible ? "В серии ✓" : "Обычный");
    }

    // ==================== Permissions ====================

    private void checkAndPromptStartupPermissions() {
        boolean needCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED;
        boolean needOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this);
        boolean needNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;

        if (needCamera || needOverlay || needNotif) {
            showPermissionsDialog(needCamera, needOverlay, needNotif);
        }
    }

    private void showPermissionsDialog(boolean needCamera, boolean needOverlay, boolean needNotif) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_permissions);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        View btnGrantCamera = dialog.findViewById(R.id.btn_grant_camera);
        View btnGrantOverlay = dialog.findViewById(R.id.btn_grant_overlay);
        MaterialButton btnDismiss = dialog.findViewById(R.id.btn_dismiss_perms);

        btnGrantCamera.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS},
                    REQ_PERMISSIONS_CAMERA
            );
            btnGrantCamera.setVisibility(View.GONE);
        });

        btnGrantOverlay.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY_PERMISSION);
            }
            btnGrantOverlay.setVisibility(View.GONE);
        });

        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMISSIONS_CAMERA) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                Toast.makeText(this, "Камера разрешена для патриотической стойки", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ==================== Tutorial ====================

    private void showTutorialDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_tutorial);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        MaterialButton btnDismiss = dialog.findViewById(R.id.btn_dismiss_tutorial);
        btnDismiss.setOnClickListener(v -> {
            StreakManager.setTutorialShown(this, true);
            dialog.dismiss();
        });
        dialog.show();
    }

    // ==================== Alarm Edit & Tutorial Trigger ====================

    private void showEditAlarmDialog(AlarmItem alarmToEdit) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_alarm_edit);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TimePicker timePicker = dialog.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);
        EditText etLabel = dialog.findViewById(R.id.et_alarm_label);

        TextView[] dayButtons = new TextView[]{
                dialog.findViewById(R.id.btn_day_mon),
                dialog.findViewById(R.id.btn_day_tue),
                dialog.findViewById(R.id.btn_day_wed),
                dialog.findViewById(R.id.btn_day_thu),
                dialog.findViewById(R.id.btn_day_fri),
                dialog.findViewById(R.id.btn_day_sat),
                dialog.findViewById(R.id.btn_day_sun)
        };

        TextView chipWeekdays = dialog.findViewById(R.id.chip_weekdays);
        TextView chipWeekends = dialog.findViewById(R.id.chip_weekends);
        TextView chipEveryday = dialog.findViewById(R.id.chip_everyday);
        TextView chipOnce = dialog.findViewById(R.id.chip_once);
        TextView tvSelectedDaysSummary = dialog.findViewById(R.id.tv_selected_days_summary);

        TextView tvNoticeDesc = dialog.findViewById(R.id.tv_streak_eligibility_desc);
        ImageView ivNoticeIcon = dialog.findViewById(R.id.iv_notice_icon);
        View layoutNotice = dialog.findViewById(R.id.layout_streak_eligibility_notice);

        boolean isNew = (alarmToEdit == null);
        AlarmItem alarm = isNew ? new AlarmItem() : alarmToEdit;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            timePicker.setHour(alarm.getHour());
            timePicker.setMinute(alarm.getMinute());
        } else {
            timePicker.setCurrentHour(alarm.getHour());
            timePicker.setCurrentMinute(alarm.getMinute());
        }
        etLabel.setText(alarm.getLabel());

        boolean[] selectedDays = new boolean[7];
        boolean[] existingDays = alarm.getDaysOfWeek();
        if (existingDays != null && existingDays.length == 7) {
            System.arraycopy(existingDays, 0, selectedDays, 0, 7);
        } else {
            // Default: weekdays
            selectedDays[0] = true;
            selectedDays[1] = true;
            selectedDays[2] = true;
            selectedDays[3] = true;
            selectedDays[4] = true;
        }

        Runnable updateDaysUI = () -> {
            for (int i = 0; i < 7; i++) {
                boolean active = selectedDays[i];
                if (active) {
                    if (i >= 5) {
                        dayButtons[i].setBackgroundResource(R.drawable.bg_day_selected_weekend);
                        dayButtons[i].setTextColor(0xFF061320);
                    } else {
                        dayButtons[i].setBackgroundResource(R.drawable.bg_day_selected_weekday);
                        dayButtons[i].setTextColor(0xFF061320);
                    }
                } else {
                    dayButtons[i].setBackgroundResource(R.drawable.bg_day_unselected);
                    dayButtons[i].setTextColor(0xFF8DB4D6);
                }
            }

            // Check matching presets
            int count = 0;
            for (boolean b : selectedDays) if (b) count++;

            boolean isWeekdays = (count == 5 && selectedDays[0] && selectedDays[1] && selectedDays[2] && selectedDays[3] && selectedDays[4]);
            boolean isWeekends = (count == 2 && selectedDays[5] && selectedDays[6]);
            boolean isEveryday = (count == 7);
            boolean isOnce = (count == 0);

            chipWeekdays.setBackgroundResource(isWeekdays ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipWeekdays.setTextColor(isWeekdays ? 0xFF33C5DB : 0xFF8DB4D6);

            chipWeekends.setBackgroundResource(isWeekends ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipWeekends.setTextColor(isWeekends ? 0xFFFEC50C : 0xFF8DB4D6);

            chipEveryday.setBackgroundResource(isEveryday ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipEveryday.setTextColor(isEveryday ? 0xFF33C5DB : 0xFF8DB4D6);

            chipOnce.setBackgroundResource(isOnce ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
            chipOnce.setTextColor(isOnce ? 0xFFFEC50C : 0xFF8DB4D6);

            // Summary text
            if (isEveryday) {
                tvSelectedDaysSummary.setText("Повтор: Каждый день (7 дней в неделю)");
            } else if (isWeekdays) {
                tvSelectedDaysSummary.setText("Повтор: Будни (Пн, Вт, Ср, Чт, Пт)");
            } else if (isWeekends) {
                tvSelectedDaysSummary.setText("Повтор: Выходные (Сб, Вс)");
            } else if (isOnce) {
                tvSelectedDaysSummary.setText("Без повтора: Сработает один раз");
            } else {
                String[] names = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                StringBuilder sb = new StringBuilder("Повтор: ");
                for (int i = 0; i < 7; i++) {
                    if (selectedDays[i]) {
                        if (sb.length() > 8) sb.append(", ");
                        sb.append(names[i]);
                    }
                }
                tvSelectedDaysSummary.setText(sb.toString());
            }
        };

        Runnable checkEligibility = () -> {
            int h = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getHour() : timePicker.getCurrentHour();
            int m = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getMinute() : timePicker.getCurrentMinute();

            boolean hasWeekday = selectedDays[0] || selectedDays[1] || selectedDays[2] || selectedDays[3] || selectedDays[4];
            boolean hasWeekend = selectedDays[5] || selectedDays[6];

            boolean weekdayEligible = (h < 10) || (h == 10 && m == 0);
            boolean weekendEligible = (h < 12) || (h == 12 && m == 0);

            boolean fullyEligible = true;
            if (hasWeekday && !weekdayEligible) fullyEligible = false;
            if (hasWeekend && !weekendEligible) fullyEligible = false;
            if (!hasWeekday && !hasWeekend) {
                fullyEligible = StreakManager.isTimeEligible(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK), h, m);
            }

            if (fullyEligible) {
                layoutNotice.setBackgroundResource(R.drawable.bg_pill_eligible);
                ivNoticeIcon.setImageResource(R.drawable.ic_check_circle);
                tvNoticeDesc.setText("Засчитывается в серию! (Будни: 00:00–10:00, Выходные: 00:00–12:00)");
                tvNoticeDesc.setTextColor(0xFF2ECC71);
            } else {
                layoutNotice.setBackgroundResource(R.drawable.bg_pill_ineligible);
                ivNoticeIcon.setImageResource(R.drawable.ic_calendar_streak);
                tvNoticeDesc.setText("Слишком поздно для серии! Будильник сработает, но серия не будет продлена (будни >10:00, вых >12:00).");
                tvNoticeDesc.setTextColor(0xFFF39C12);
            }
        };

        timePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> checkEligibility.run());

        // Individual day buttons click listeners
        for (int i = 0; i < 7; i++) {
            final int dayIdx = i;
            dayButtons[dayIdx].setOnClickListener(v -> {
                selectedDays[dayIdx] = !selectedDays[dayIdx];
                updateDaysUI.run();
                checkEligibility.run();
            });
        }

        // Preset chip listeners
        chipWeekdays.setOnClickListener(v -> {
            for (int i = 0; i < 5; i++) selectedDays[i] = true;
            selectedDays[5] = false;
            selectedDays[6] = false;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipWeekends.setOnClickListener(v -> {
            for (int i = 0; i < 5; i++) selectedDays[i] = false;
            selectedDays[5] = true;
            selectedDays[6] = true;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipEveryday.setOnClickListener(v -> {
            for (int i = 0; i < 7; i++) selectedDays[i] = true;
            updateDaysUI.run();
            checkEligibility.run();
        });

        chipOnce.setOnClickListener(v -> {
            for (int i = 0; i < 7; i++) selectedDays[i] = false;
            updateDaysUI.run();
            checkEligibility.run();
        });

        updateDaysUI.run();
        checkEligibility.run();

        dialog.findViewById(R.id.btn_close_dialog).setOnClickListener(v -> dialog.dismiss());

        dialog.findViewById(R.id.btn_save_alarm).setOnClickListener(v -> {
            int h = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getHour() : timePicker.getCurrentHour();
            int m = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? timePicker.getMinute() : timePicker.getCurrentMinute();

            alarm.setHour(h);
            alarm.setMinute(m);
            alarm.setLabel(etLabel.getText().toString().trim().isEmpty() ? "Патриотический подъем" : etLabel.getText().toString().trim());
            alarm.setDaysOfWeek(Arrays.copyOf(selectedDays, 7));
            alarm.setEnabled(true);

            if (isNew) {
                AlarmStorage.addAlarm(this, alarm);
                alarmList.add(0, alarm);
                alarmsAdapter.notifyItemInserted(0);
                if (recyclerAlarms != null) recyclerAlarms.scrollToPosition(0);
            } else {
                AlarmStorage.updateAlarm(this, alarm);
                alarmsAdapter.notifyDataSetChanged();
            }

            updateStreakUI();
            dialog.dismiss();

            // Automatically show tutorial after setting an alarm
            showTutorialDialog();
        });

        dialog.show();
    }

    // ==================== 365 Days Roadmap ====================

    private void showRoadmapDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_roadmap_365);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        RecyclerView recyclerTiers = dialog.findViewById(R.id.recycler_tiers);
        recyclerTiers.setLayoutManager(new LinearLayoutManager(this));
        int currentStreak = StreakManager.getCurrentStreak(this);

        RoadmapAdapter adapter = new RoadmapAdapter(StreakTier.getAllTiers(), currentStreak, tier -> {
            dialog.dismiss();
        });
        recyclerTiers.setAdapter(adapter);

        dialog.findViewById(R.id.btn_close_roadmap).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_dismiss_roadmap).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // ==================== AlarmsAdapter.OnAlarmListener ====================

    @Override
    public void onToggle(AlarmItem alarm, boolean enabled) {
        AlarmStorage.updateAlarm(this, alarm);
        updateStreakUI();
    }

    @Override
    public void onDelete(AlarmItem alarm) {
        int idx = alarmList.indexOf(alarm);
        if (idx >= 0) {
            alarmList.remove(idx);
            alarmsAdapter.notifyItemRemoved(idx);
            AlarmStorage.deleteAlarm(this, alarm.getId());
            updateStreakUI();
            Toast.makeText(this, "Будильник удален", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEdit(AlarmItem alarm) {
        showEditAlarmDialog(alarm);
    }
}
