package com.example.studentlifeos;

/**
 * One recurring weekly class slot. Matches the "timetable_entries" Firestore collection:
 * studentId, dayIndex (1=Monday..7=Sunday), dayName, startTime/endTime ("HH:mm", 24h,
 * zero-padded so they sort and compare correctly as plain strings), subjectName,
 * professorName, room.
 */
public class TimetableEntry {
    public String id;
    public int dayIndex;       // 1=Monday ... 7=Sunday
    public String dayName;     // "Monday" ... "Sunday"
    public String startTime;   // "09:00"
    public String endTime;     // "10:00"
    public String subjectName;
    public String professorName;
    public String room;

    public TimetableEntry() {
    }

    public TimetableEntry(int dayIndex, String dayName, String startTime, String endTime,
                           String subjectName, String professorName, String room) {
        this.dayIndex = dayIndex;
        this.dayName = dayName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectName = subjectName;
        this.professorName = professorName;
        this.room = room;
    }

    public static final String[] DAY_NAMES = {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    /** Best-effort match of a model/user-typed day string to 1..7 (Monday..Sunday). Returns 0 if unrecognized. */
    public static int parseDayIndex(String raw) {
        if (raw == null) return 0;
        String s = raw.trim().toLowerCase(java.util.Locale.US);
        for (int i = 0; i < DAY_NAMES.length; i++) {
            String full = DAY_NAMES[i].toLowerCase(java.util.Locale.US);
            if (s.equals(full) || s.equals(full.substring(0, 3)) || full.startsWith(s)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static String dayNameFor(int dayIndex) {
        return (dayIndex >= 1 && dayIndex <= 7) ? DAY_NAMES[dayIndex - 1] : "";
    }
}
