package kz.patriotic.alarm.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages language switching (Russian / Kazakh) and the optional 100-day language learning immersion.
 * Ensures that choosing Russian genuinely keeps the entire UI in Russian,
 * and choosing Kazakh genuinely keeps the entire UI in Kazakh without breaking the interface.
 */
public class LanguageProgressionManager {
    private static final String PREF_NAME = "patriotic_lang_prefs";
    private static final String KEY_SELECTED_LANG = "selected_language"; // "ru" or "kz"
    private static final String KEY_PROGRESSION_ENABLED = "progression_learning_enabled"; // default false so Russian is not overwritten

    public static String getLanguage(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SELECTED_LANG, "ru");
    }

    public static boolean isKazakh(Context context) {
        return "kz".equals(getLanguage(context));
    }

    public static void setLanguage(Context context, String lang) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SELECTED_LANG, lang).apply();
    }

    public static boolean isProgressionEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_PROGRESSION_ENABLED, false);
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
     * Reliable localization method:
     * - Returns kzText if Kazakh is selected.
     * - Returns ruText if Russian is selected.
     * Guaranteed never to break Russian or Kazakh.
     */
    public static String getLocalizedText(Context context, String ruText, String kzText) {
        return isKazakh(context) ? kzText : ruText;
    }

    /**
     * Backward-compatible overload.
     */
    public static String getLocalizedText(Context context, String ruText, String kzText, int minStreakThreshold) {
        return getLocalizedText(context, ruText, kzText);
    }

    /**
     * Educational phrase for the 100-day language learning feature.
     */
    public static String getDailyImmersionPhrase(Context context) {
        int streak = StreakManager.getCurrentStreak(context);
        String[] wordsKz = {
                "Сәлемдесу (Приветствие)", "Отан (Родина)", "Ел (Страна)", "Жүрек (Сердце)", "Әнұран (Гимн)",
                "Ту (Флаг)", "Елтаңба (Герб)", "Батыр (Герой)", "Тұлпар (Скакун)", "Қыран (Беркут)",
                "Бейбітшілік (Мир)", "Бірлік (Единство)", "Достық (Дружба)", "Атамекен (Земля предков)", "Азаттық (Свобода)"
        };
        int idx = (streak - 1) % wordsKz.length;
        if (idx < 0) idx = 0;
        return wordsKz[idx];
    }
}
