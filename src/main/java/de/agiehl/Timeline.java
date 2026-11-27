package de.agiehl;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.stream.Collectors;

public final class Timeline {
    public record PlayedGame(String title, String cover, long rounds) {}
    public record Day(String date, List<PlayedGame> games) {}
    public record Cell(String month, int days, String level) {
        public String label() {
            return Formats.unit(days, "Spieltag", "Spieltage");
        }
    }
    public record Year(int number, List<Cell> months, List<Day> days) {}

    private Timeline() {}

    static List<Year> build(NavigableMap<LocalDate, List<Report.Entry>> calendar, Map<Integer, String> covers) {
        var years = new ArrayList<Year>();
        for (int year = calendar.lastKey().getYear(); year >= calendar.firstKey().getYear(); year--) {
            var months = new ArrayList<Cell>();
            var days = new ArrayList<Day>();
            for (var month : Month.values()) {
                var start = LocalDate.of(year, month, 1);
                var played = calendar.subMap(start, true, start.plusMonths(1), false);
                int count = played.size();
                var level = switch (count) {
                    case 0 -> "empty";
                    case 1 -> "low";
                    case 2, 3 -> "medium";
                    default -> "high";
                };
                months.add(new Cell(month.getDisplayName(TextStyle.FULL, Locale.GERMAN), count, level));
                played.forEach((date, entries) -> days.add(new Day(Formats.date(date), dayGames(entries, covers))));
            }
            years.add(new Year(year, months, days.reversed()));
        }
        return years;
    }

    private static List<PlayedGame> dayGames(List<Report.Entry> entries, Map<Integer, String> covers) {
        return entries.stream().collect(Collectors.groupingBy(Report.Entry::gameId)).entrySet().stream()
                .map(group -> new PlayedGame(group.getValue().getFirst().title(), covers.get(group.getKey()),
                        Report.count(group.getValue())))
                .sorted(Comparator.comparing(PlayedGame::title)).toList();
    }
}
