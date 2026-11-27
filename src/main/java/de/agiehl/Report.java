package de.agiehl;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record Report(List<Game> games, List<Person> people, List<Timeline.Year> years, long rounds,
                     long minutes, int playedDays, String firstDate, String lastDate, String span) {
    public record Entry(LocalDate date, int gameId, String title, int quantity, int minutes, List<String> players) {}
    public record Game(int id, String title, long rounds, long minutes, String cover) {
        public String duration() {
            return Formats.duration(minutes);
        }
    }
    public record Person(String name, long rounds) {}

    static Report from(List<Entry> entries, Map<Integer, String> covers) {
        if (entries.isEmpty()) {
            return new Report(List.of(), List.of(), List.of(), 0, 0, 0, "", "", "");
        }
        var byGame = entries.stream().collect(Collectors.groupingBy(Entry::gameId));
        var games = byGame.entrySet().stream().map(group -> {
            var plays = group.getValue();
            return new Game(group.getKey(), plays.getFirst().title(), count(plays),
                    plays.stream().mapToLong(Entry::minutes).sum(), covers.get(group.getKey()));
        }).sorted(Comparator.comparingLong(Game::rounds).reversed().thenComparing(Game::title)
                .thenComparingInt(Game::id)).toList();

        var attendance = new HashMap<String, Long>();
        entries.forEach(play -> play.players().forEach(name -> attendance.merge(name, (long) play.quantity(), Long::sum)));
        var people = attendance.entrySet().stream().map(e -> new Person(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Person::rounds).reversed().thenComparing(Person::name)).toList();

        var days = entries.stream().collect(Collectors.groupingBy(Entry::date, TreeMap::new, Collectors.toList()));
        var first = days.firstKey();
        var last = days.lastKey();
        return new Report(games, people, Timeline.build(days, covers), count(entries),
                games.stream().mapToLong(Game::minutes).sum(), days.size(),
                Formats.date(first), Formats.date(last), Formats.period(Period.between(first, last)));
    }

    static long count(List<Entry> entries) {
        return entries.stream().mapToLong(Entry::quantity).sum();
    }

    public String duration() {
        return Formats.duration(minutes);
    }
}
