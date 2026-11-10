package kz.patriotic.alarm.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.RemoteViews;

import java.util.List;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.model.AlarmItem;
import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.ui.MainActivity;
import kz.patriotic.alarm.util.AlarmStorage;
import kz.patriotic.alarm.util.StreakManager;

public class PatrioticAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
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

        // Update texts
        views.setTextViewText(R.id.widget_streak_count, streak + " ДНЕЙ");
        views.setTextViewText(R.id.widget_tier_title, "Ранг: " + tier.getKzTitle() + " (" + tier.getRuTitle() + ")");
        try {
            views.setTextColor(R.id.widget_tier_title, Color.parseColor(tier.getAccentColorHex()));
        } catch (Exception ignored) {}

        views.setTextViewText(R.id.widget_misses, missesRemaining + "/2 запас");
        views.setTextViewText(R.id.widget_next_alarm, nextAlarmText);
        views.setTextViewText(R.id.widget_status_badge, isEligible ? "В серии ✓" : "Обычный");

        // Intent to open MainActivity on click
        Intent intent = new Intent(context, MainActivity.class);
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
