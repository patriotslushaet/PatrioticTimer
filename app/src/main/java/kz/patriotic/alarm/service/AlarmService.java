package kz.patriotic.alarm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.ui.AlarmActivity;

public class AlarmService extends Service {
    private static final String CHANNEL_ID = "patriotic_alarm_channel_v1";
    private static final int NOTIFICATION_ID = 1991;
    private Vibrator vibrator;
    private final Handler autoStopHandler = new Handler(Looper.getMainLooper());
    private Runnable autoStopRunnable;

    /**
     * Safely stops the alarm service and cancels all active vibrations across the app.
     */
    public static void stopAlarm(Context context) {
        if (context == null) return;
        try {
            context.stopService(new Intent(context, AlarmService.class));
        } catch (Exception ignored) {}

        try {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null) {
                v.cancel();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String label = (intent != null && intent.hasExtra("alarm_label"))
                ? intent.getStringExtra("alarm_label")
                : "Патриотический Будильник Казахстан";

        Intent fullScreenIntent = new Intent(this, AlarmActivity.class);
        if (intent != null && intent.getExtras() != null) {
            fullScreenIntent.putExtras(intent.getExtras());
        }
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_kazakh_eagle_sun)
                .setContentTitle("Патриот, оян!")
                .setContentText(label)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // Controlled vibration pulse for initial wake-up
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 600, 400, 600, 400};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception ignored) {}

        // Safety auto-cancel after 30 seconds so vibration never hangs indefinitely
        autoStopRunnable = () -> {
            stopAlarm(this);
            stopSelf();
        };
        autoStopHandler.postDelayed(autoStopRunnable, 30000L);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (autoStopHandler != null && autoStopRunnable != null) {
            autoStopHandler.removeCallbacks(autoStopRunnable);
        }
        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Exception ignored) {}
        }
        try {
            stopForeground(true);
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Патриотический Будильник",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Срабатывание патриотического будильника с гимном");
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
