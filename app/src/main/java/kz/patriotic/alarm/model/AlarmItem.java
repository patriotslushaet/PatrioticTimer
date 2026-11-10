package kz.patriotic.alarm.model;

import java.util.Calendar;
import java.util.Locale;

public class AlarmItem {
    private long id;
    private int hour;
    private int minute;
    private boolean[] daysOfWeek; // Index 0..6: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
    private boolean isEnabled;
    private String label;

    public AlarmItem() {
        this.id = System.currentTimeMillis();
        this.hour = 7;
        this.minute = 0;
        this.daysOfWeek = new boolean[]{true, true, true, true, true, false, false}; // Mon-Fri default
        this.isEnabled = true;
        this.label = "Патриотический подъем";
    }

    public AlarmItem(long id, int hour, int minute, boolean[] daysOfWeek, boolean isEnabled, String label) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.daysOfWeek = daysOfWeek != null ? daysOfWeek : new boolean[7];
        this.isEnabled = isEnabled;
        this.label = label;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public boolean[] getDaysOfWeek() { return daysOfWeek; }
    public void setDaysOfWeek(boolean[] daysOfWeek) { this.daysOfWeek = daysOfWeek; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    /**
     * Checks if a given day index (0=Mon, ..., 6=Sun) with the current time qualifies for the streak.
     * Rule: Mon-Fri between 00:00 and 10:00, Sat-Sun between 00:00 and 12:00.
     */
    public boolean isStreakEligibleForDayIndex(int dayIndex) {
        boolean isWeekend = (dayIndex == 5 || dayIndex == 6); // Sat or Sun
        if (!isWeekend) {
            return hour < 10 || (hour == 10 && minute == 0);
        } else {
            return hour < 12 || (hour == 12 && minute == 0);
        }
    }

    /**
     * Checks if this alarm qualifies for the streak today or on its active days.
     */
    public boolean hasEligibleDays() {
        boolean anySelected = false;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeek[i]) {
                anySelected = true;
                if (isStreakEligibleForDayIndex(i)) {
                    return true;
                }
            }
        }
        if (!anySelected) {
            // One-off alarm today
            Calendar cal = Calendar.getInstance();
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int dayIdx = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - 2);
            return isStreakEligibleForDayIndex(dayIdx);
        }
        return false;
    }

    /**
     * Converts Calendar.DAY_OF_WEEK to 0=Mon..6=Sun
     */
    public static int calendarDayToDayIndex(int calendarDay) {
        if (calendarDay == Calendar.SUNDAY) return 6;
        return calendarDay - 2;
    }

    public String getDaysDisplayString() {
        String[] dayNames = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        int count = 0;
        for (boolean d : daysOfWeek) if (d) count++;

        if (count == 7) return "Каждый день";
        if (count == 0) return "Один раз";
        if (count == 5 && daysOfWeek[0] && daysOfWeek[1] && daysOfWeek[2] && daysOfWeek[3] && daysOfWeek[4]) {
            return "Будни (Пн-Пт)";
        }
        if (count == 2 && daysOfWeek[5] && daysOfWeek[6]) {
            return "Выходные (Сб-Вс)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (daysOfWeek[i]) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(dayNames[i]);
            }
        }
        return sb.toString();
    }
}
