package kz.patriotic.alarm.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages language switching (Russian / Kazakh) and the gradual 100-day language learning immersion:
 * For Russian language users, the interface dynamically transitions into Kazakh word by word over 100 days
 * as the user maintains their morning patriotic alarm streak!
 */
public class LanguageProgressionManager {
    private static final String PREF_NAME = "patriotic_lang_prefs";
    private static final String KEY_SELECTED_LANG = "selected_language"; // "ru" or "kz"
    private static final String KEY_PROGRESSION_ENABLED = "progression_learning_enabled"; // default true

    public static String getLanguage(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_LANG, "ru");
    }

    public static void setLanguage(Context context, String lang) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SELECTED_LANG, lang).apply();
    }

    public static boolean isProgressionEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_PROGRESSION_ENABLED, true);
    }

    public static void setProgressionEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_PROGRESSION_ENABLED, enabled).apply();
    }

    public static int getLearningProgressPercent(Context context) {
        int streak = StreakManager.getCurrentStreak(context);
        return Math.min(100, Math.max(1, streak));
    }

    /**
     * If user explicitly selected Kazakh -> returns kzText.
     * If user selected Russian:
     *   - If 100-day progression is enabled and streak >= minStreakThreshold -> returns kzText (gradual immersion!)
     *   - Otherwise -> returns ruText.
     */
    public static String getLocalizedText(Context context, String ruText, String kzText, int minStreakThreshold) {
        String lang = getLanguage(context);
        if ("kz".equals(lang)) {
            return kzText;
        }

        if (isProgressionEnabled(context)) {
            int streak = StreakManager.getCurrentStreak(context);
            if (streak >= minStreakThreshold) {
                return kzText;
            }
        }
        return ruText;
    }
}
