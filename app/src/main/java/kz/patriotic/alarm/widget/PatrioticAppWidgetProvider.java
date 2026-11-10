package kz.patriotic.alarm.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.AlarmItem;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.ui.MainActivity;
import kz.patriotic.alarm.util.AlarmStorage;
import kz.patriotic.alarm.util.StreakManager;

public class PatrioticAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "PatrioticWidget";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, PatrioticAppWidgetProvider.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            if (ids != null && ids.length > 0) {
                onUpdate(context, mgr, ids);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage());
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        if (appWidgetIds == null) return;
        for (int appWidgetId : appWidgetIds) {
            try {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            } catch (Exception e) {
                Log.e(TAG, "Failed updating widget id " + appWidgetId + ": " + e.getMessage());
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        int streak = StreakManager.getCurrentStreak(context);
        StreakTier tier = StreakTier.getTierForDays(streak);
        int missesRemaining = StreakManager.getMonthlyMissesRemaining(context);

        List<AlarmItem> alarms = AlarmStorage.getAlarms(context);
        String nextAlarmText = "Будильник не установлен";
        boolean isEligible = false;
        for (AlarmItem a : alarms) {
            if (a.isEnabled()) {
                nextAlarmText = "Будильник: " + a.getFormattedTime();
                isEligible = a.hasEligibleDays();
                break;
            }
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_patriotic);

        // Ensure real flag PNG is set
        views.setImageViewResource(R.id.widget_flag, R.drawable.flag_kz);

        // Texts
        views.setTextViewText(R.id.widget_streak_count, streak + " ДНЕЙ");
        views.setTextViewText(R.id.widget_tier_title, "Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ")");
        try {
            views.setTextColor(R.id.widget_tier_title, Color.parseColor(tier.getAccentColorHex()));
        } catch (Exception ignored) {}

        views.setTextViewText(R.id.widget_misses, missesRemaining + "/2 запас");
        views.setTextViewText(R.id.widget_next_alarm, nextAlarmText);
        views.setTextViewText(R.id.widget_status_badge, isEligible ? "В серии ✓" : "Обычный");

        // Click on widget opens MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
