package kz.patriotic.alarm.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kz.patriotic.alarm.model.AlarmItem;

public class AlarmStorage {
    private static final String PREF_NAME = "patriotic_alarms_storage";
    private static final String KEY_ALARMS = "alarms_list_json";
    private static final Gson gson = new Gson();

    public static List<AlarmItem> getAlarms(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_ALARMS, null);
        if (json == null || json.trim().isEmpty()) {
            List<AlarmItem> defaults = createDefaultAlarms();
            saveAlarms(context, defaults);
            return defaults;
        }

        try {
            Type listType = new TypeToken<ArrayList<AlarmItem>>() {}.getType();
            List<AlarmItem> list = gson.fromJson(json, listType);
            if (list == null || list.isEmpty()) {
                list = createDefaultAlarms();
                saveAlarms(context, list);
            }
            return list;
        } catch (Exception e) {
            List<AlarmItem> defaults = createDefaultAlarms();
            saveAlarms(context, defaults);
            return defaults;
        }
    }

    public static void saveAlarms(Context context, List<AlarmItem> alarms) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(alarms);
        sp.edit().putString(KEY_ALARMS, json).apply();
        StreakManager.updateWidgets(context);
    }

    public static void addAlarm(Context context, AlarmItem alarm) {
        List<AlarmItem> list = getAlarms(context);
        list.add(0, alarm);
        saveAlarms(context, list);
        AlarmScheduler.scheduleNextAlarm(context);
    }

    public static void updateAlarm(Context context, AlarmItem updatedAlarm) {
        List<AlarmItem> list = getAlarms(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == updatedAlarm.getId()) {
                list.set(i, updatedAlarm);
                break;
            }
        }
        saveAlarms(context, list);
        AlarmScheduler.scheduleNextAlarm(context);
    }

    public static void deleteAlarm(Context context, long alarmId) {
        List<AlarmItem> list = getAlarms(context);
        list.removeIf(a -> a.getId() == alarmId);
        saveAlarms(context, list);
        AlarmScheduler.scheduleNextAlarm(context);
    }

    private static List<AlarmItem> createDefaultAlarms() {
        List<AlarmItem> list = new ArrayList<>();
        // Default weekday alarm: 06:45 Mon-Fri
        list.add(new AlarmItem(1001, 6, 45, new boolean[]{true, true, true, true, true, false, false}, true, "Ұлттық Рух (Будни)"));
        // Default weekend alarm: 08:30 Sat-Sun
        list.add(new AlarmItem(1002, 8, 30, new boolean[]{false, false, false, false, false, true, true}, true, "Патриот Демалысы (Выходные)"));
        return list;
    }
}
