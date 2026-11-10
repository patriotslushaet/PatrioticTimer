package kz.patriotic.alarm.util;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kz.patriotic.alarm.model.StreakTier;
import kz.patriotic.alarm.widget.PatrioticAppWidgetProvider;

public class StreakManager {
    private static final String PREF_NAME = "patriotic_streak_prefs";
    private static final String KEY_CURRENT_STREAK = "current_streak";
    private static final String KEY_HIGHEST_STREAK = "highest_streak";
    private static final String KEY_LAST_DATE = "last_completion_date";
    private static final String KEY_MONTHLY_MISSES = "monthly_misses_used";
    private static final String KEY_LAST_MONTH = "last_recorded_month";
    private static final String KEY_TUTORIAL_SHOWN = "tutorial_shown";

    public static int getCurrentStreak(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_CURRENT_STREAK, 1); // Start with 1 on initial setup
    }

    public static int getHighestStreak(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_HIGHEST_STREAK, 1);
    }

    public static int getMonthlyMissesUsed(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        checkAndResetMonthlyMisses(sp);
        return sp.getInt(KEY_MONTHLY_MISSES, 0);
    }

    public static int getMonthlyMissesRemaining(Context context) {
        return Math.max(0, 2 - getMonthlyMissesUsed(context));
    }

    public static StreakTier getCurrentTier(Context context) {
        int streak = getCurrentStreak(context);
        return StreakTier.getTierForDays(streak);
    }

    public static boolean isTutorialShown(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_TUTORIAL_SHOWN, false);
    }

    public static void setTutorialShown(Context context, boolean shown) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_TUTORIAL_SHOWN, shown).apply();
    }

    private static void checkAndResetMonthlyMisses(SharedPreferences sp) {
        Calendar now = Calendar.getInstance();
        int currentMonth = now.get(Calendar.MONTH);
        int lastMonth = sp.getInt(KEY_LAST_MONTH, -1);
        if (lastMonth != currentMonth) {
            sp.edit()
                    .putInt(KEY_LAST_MONTH, currentMonth)
                    .putInt(KEY_MONTHLY_MISSES, 0)
                    .apply();
        }
    }

    /**
     * Checks if current time is eligible to maintain the streak.
     * Weekdays: 00:00 - 10:00. Weekends: 00:00 - 12:00.
     */
    public static boolean isTimeEligibleNow() {
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        return isTimeEligible(dayOfWeek, hour, minute);
    }

    public static boolean isTimeEligible(int dayOfWeek, int hour, int minute) {
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY);
        if (!isWeekend) {
            return hour < 10 || (hour == 10 && minute == 0);
        } else {
            return hour < 12 || (hour == 12 && minute == 0);
        }
    }

    /**
     * Records a successful alarm & pose completion.
     * Evaluates missed days and applies the 2-misses-per-month grace rule.
     */
    public static void recordCompletion(Context context, boolean forceStreakIncrease) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        checkAndResetMonthlyMisses(sp);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDate = sp.getString(KEY_LAST_DATE, "");

        int streak = sp.getInt(KEY_CURRENT_STREAK, 1);
        int highest = sp.getInt(KEY_HIGHEST_STREAK, 1);
        int missesUsed = sp.getInt(KEY_MONTHLY_MISSES, 0);

        if (!today.equals(lastDate)) {
            if (lastDate.isEmpty() || forceStreakIncrease) {
                streak += 1;
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date dLast = sdf.parse(lastDate);
                    Date dToday = sdf.parse(today);
                    long diffDays = (dToday.getTime() - dLast.getTime()) / (1000 * 60 * 60 * 24);

                    if (diffDays == 1) {
                        // Consecutive day
                        streak += 1;
                    } else if (diffDays > 1) {
                        int missedDays = (int) (diffDays - 1);
                        int availableMisses = 2 - missesUsed;
                        if (missedDays <= availableMisses) {
                            // Covered by monthly grace skips!
                            missesUsed += missedDays;
                            streak += 1; // Preserved and increased!
                        } else {
                            // Exceeded 2 misses this month -> reset
                            missesUsed = 2;
                            streak = 1;
                        }
                    }
                } catch (Exception e) {
                    streak += 1;
                }
            }

            if (streak > highest) {
                highest = streak;
            }

            sp.edit()
                    .putString(KEY_LAST_DATE, today)
                    .putInt(KEY_CURRENT_STREAK, streak)
                    .putInt(KEY_HIGHEST_STREAK, highest)
                    .putInt(KEY_MONTHLY_MISSES, missesUsed)
                    .apply();

            updateWidgets(context);
        }
    }

    /**
     * Helper to set a simulated streak for testing all 365 days tiers and widgets.
     */
    public static void setSimulatedStreak(Context context, int newStreak) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int highest = Math.max(sp.getInt(KEY_HIGHEST_STREAK, 1), newStreak);
        sp.edit()
                .putInt(KEY_CURRENT_STREAK, newStreak)
                .putInt(KEY_HIGHEST_STREAK, highest)
                .apply();
        updateWidgets(context);
    }

    public static void updateWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            // 1. Horizontal widget (image_3)
            ComponentName horizWidget = new ComponentName(context, PatrioticAppWidgetProvider.class);
            int[] horizIds = appWidgetManager.getAppWidgetIds(horizWidget);
            if (horizIds != null && horizIds.length > 0) {
                Intent intent = new Intent(context, PatrioticAppWidgetProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, horizIds);
                context.sendBroadcast(intent);
            }

            // 2. Square widget (image_4)
            ComponentName sqWidget = new ComponentName(context, kz.patriotic.alarm.widget.PatrioticWidgetSquareProvider.class);
            int[] sqIds = appWidgetManager.getAppWidgetIds(sqWidget);
            if (sqIds != null && sqIds.length > 0) {
                Intent intent = new Intent(context, kz.patriotic.alarm.widget.PatrioticWidgetSquareProvider.class);
                intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, sqIds);
                context.sendBroadcast(intent);
            }
        } catch (Exception ignored) {}
    }
}
