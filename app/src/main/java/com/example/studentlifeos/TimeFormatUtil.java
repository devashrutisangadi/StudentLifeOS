package com.example.studentlifeos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Converts between the "HH:mm" (24h, zero-padded) strings stored in Firestore and
 *  human-readable "h:mm a" display strings. 24h format is used for storage because it
 *  sorts and compares correctly as a plain string. */
public class TimeFormatUtil {

    private static final SimpleDateFormat STORAGE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    public static String toDisplay(String storageTime) {
        if (storageTime == null || storageTime.trim().isEmpty()) return "";
        try {
            Date date = STORAGE_FORMAT.parse(storageTime.trim());
            return date != null ? DISPLAY_FORMAT.format(date) : storageTime;
        } catch (ParseException e) {
            return storageTime; // fall back to whatever the model/user typed
        }
    }

    public static String formatRange(String startTime, String endTime) {
        return toDisplay(startTime) + " – " + toDisplay(endTime);
    }

    /** Current time as "HH:mm" — safe to lexicographically compare against stored times. */
    public static String nowAsStorageTime() {
        return STORAGE_FORMAT.format(new Date());
    }

    /** Today as 1(Monday)..7(Sunday), matching TimetableEntry.dayIndex. */
    public static int todayDayIndex() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int calDow = cal.get(java.util.Calendar.DAY_OF_WEEK); // Sunday=1 ... Saturday=7
        return calDow == java.util.Calendar.SUNDAY ? 7 : calDow - 1;
    }
}
