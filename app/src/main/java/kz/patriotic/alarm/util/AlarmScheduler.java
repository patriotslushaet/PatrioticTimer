package kz.patriotic.alarm.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;

import kz.patriotic.alarm.model.AlarmItem;
import kz.patriotic.alarm.receiver.AlarmReceiver;
import kz.patriotic.alarm.ui.MainActivity;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private static final int REQUEST_CODE = 9911;

    public static void scheduleNextAlarm(Context context) {
        List<AlarmItem> alarms = AlarmStorage.getAlarms(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        long nextTriggerMillis = Long.MAX_VALUE;
        AlarmItem nextAlarm = null;

        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();

        for (AlarmItem alarm : alarms) {
            if (!alarm.isEnabled()) continue;

            // Check next 7 days for this alarm
            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                Calendar testCal = Calendar.getInstance();
                testCal.add(Calendar.DAY_OF_YEAR, dayOffset);
                testCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
                testCal.set(Calendar.MINUTE, alarm.getMinute());
                testCal.set(Calendar.SECOND, 0);
                testCal.set(Calendar.MILLISECOND, 0);

                if (testCal.getTimeInMillis() <= nowMillis) {
                    continue;
                }

                int dayOfWeek = testCal.get(Calendar.DAY_OF_WEEK);
                int dayIndex = AlarmItem.calendarDayToDayIndex(dayOfWeek);

                boolean isActiveOnDay = false;
                boolean anyDaySelected = false;
                for (boolean d : alarm.getDaysOfWeek()) {
                    if (d) { anyDaySelected = true; break; }
                }

                if (!anyDaySelected) {
                    // One-off alarm
                    isActiveOnDay = true;
                } else if (dayIndex >= 0 && dayIndex < 7 && alarm.getDaysOfWeek()[dayIndex]) {
                    isActiveOnDay = true;
                }

                if (isActiveOnDay) {
                    long trigger = testCal.getTimeInMillis();
                    if (trigger < nextTriggerMillis) {
                        nextTriggerMillis = trigger;
                        nextAlarm = alarm;
                    }
                    break;
                }
            }
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("kz.patriotic.alarm.ACTION_TRIGGER_ALARM");

        if (nextAlarm != null && nextTriggerMillis != Long.MAX_VALUE) {
            intent.putExtra("alarm_id", nextAlarm.getId());
            intent.putExtra("alarm_label", nextAlarm.getLabel());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Intent showIntent = new Intent(context, MainActivity.class);
            PendingIntent showPendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(
                    nextTriggerMillis,
                    showPendingIntent
            );

            try {
                alarmManager.setAlarmClock(clockInfo, pendingIntent);
                Log.d(TAG, "Scheduled next alarm for: " + nextTriggerMillis);
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException scheduling alarm: " + e.getMessage());
            }
        } else {
            // Cancel existing alarm
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    public static void triggerInstantAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("kz.patriotic.alarm.ACTION_TRIGGER_ALARM");
        intent.putExtra("is_demo", true);
        intent.putExtra("alarm_label", "Патриотический Демо-Будильник");
        context.sendBroadcast(intent);
    }
}
