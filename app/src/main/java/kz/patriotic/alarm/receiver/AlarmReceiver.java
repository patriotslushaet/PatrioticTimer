package kz.patriotic.alarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import kz.patriotic.alarm.service.AlarmService;
import kz.patriotic.alarm.ui.AlarmActivity;
import kz.patriotic.alarm.util.AlarmScheduler;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered! Waking up device...");

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "PatrioticAlarm:WakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L /* 10 minutes */);
        }

        // Start Foreground Service for reliable alarm ringing
        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtras(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Also launch AlarmActivity directly
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.putExtras(intent);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            context.startActivity(activityIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch activity directly: " + e.getMessage());
        }

        // Reschedule next repeating alarm
        AlarmScheduler.scheduleNextAlarm(context);
    }
}
