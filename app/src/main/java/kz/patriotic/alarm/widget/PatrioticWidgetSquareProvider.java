package kz.patriotic.alarm.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import kz.patriotic.alarm.R;
import kz.patriotic.alarm.ui.MainActivity;
import kz.patriotic.alarm.util.StreakManager;

public class PatrioticWidgetSquareProvider extends AppWidgetProvider {
    private static final String TAG = "PatrioticWidgetSquare";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        try {
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, PatrioticWidgetSquareProvider.class);
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
                Log.e(TAG, "Failed updating square widget id " + appWidgetId + ": " + e.getMessage());
            }
        }
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        int streak = StreakManager.getCurrentStreak(context);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_square);
        views.setTextViewText(R.id.tv_widget_sq_streak_num, String.valueOf(streak));
        views.setTextViewText(R.id.tv_widget_sq_streak_unit, "КҮН СЕРИЯСЫ");

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_square_root, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
