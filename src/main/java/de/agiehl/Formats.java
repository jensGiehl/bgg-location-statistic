package de.agiehl;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

final class Formats {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMAN);

    private Formats() {}

    static String date(LocalDate date) {
        return DATE.format(date);
    }

    static String period(Period period) {
        return String.join(", ", unit(period.getYears(), "Jahr", "Jahre"), unit(period.getMonths(), "Monat", "Monate"))
                + " und " + unit(period.getDays(), "Tag", "Tage");
    }

    static String duration(long minutes) {
        var parts = new ArrayList<String>();
        long days = minutes / 1440;
        long hours = minutes % 1440 / 60;
        if (days > 0) parts.add(unit(days, "Tag", "Tage"));
        if (hours > 0) parts.add(unit(hours, "Stunde", "Stunden"));
        if (minutes % 60 > 0 || parts.isEmpty()) parts.add(unit(minutes % 60, "Minute", "Minuten"));
        var last = parts.removeLast();
        return parts.isEmpty() ? last : String.join(", ", parts) + " und " + last;
    }

    static String unit(long value, String singular, String plural) {
        return value + " " + (value == 1 ? singular : plural);
    }
}
