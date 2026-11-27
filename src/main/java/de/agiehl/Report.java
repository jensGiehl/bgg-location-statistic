package de.agiehl;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public record Report(List<Game> games, List<Person> people, List<Timeline.Year> years, long rounds,
                     long minutes, int playedDays, String firstDate, String lastDate, String span) {
    public record Entry(Integer playId, LocalDate date, int gameId, String title, int quantity, int minutes,
                        Map<String, Boolean> winsByPlayer) {
        public String dateLabel() {
            return Formats.date(date);
        }

        public String duration() {
            return minutes > 0 ? Formats.duration(minutes) : "Dauer nicht erfasst";
        }
    }

    public record Game(int id, String title, long rounds, long playedDays, long minutes, String cover) {
        public String duration() {
            return Formats.duration(minutes);
        }
    }
    public record Participation(Entry play, boolean won) {}
    public record Person(String name, List<Participation> history, long rounds) {}

    static Report from(List<Entry> entries, Map<Integer, String> covers) {
        if (entries.isEmpty()) {
            return new Report(List.of(), List.of(), List.of(), 0, 0, 0, "", "", "");
        }
        var byGame = entries.stream().collect(Collectors.groupingBy(Entry::gameId));
        var games = byGame.entrySet().stream().map(group -> {
            var plays = group.getValue();
            return new Game(group.getKey(), plays.getFirst().title(), count(plays),
                    plays.stream().map(Entry::date).distinct().count(),
                    plays.stream().mapToLong(Entry::minutes).sum(), covers.get(group.getKey()));
        }).sorted(Comparator.comparingLong(Game::playedDays).reversed().thenComparing(Game::title)
                .thenComparingInt(Game::id)).toList();

        var days = entries.stream().collect(Collectors.groupingBy(Entry::date, TreeMap::new, Collectors.toList()));
        var first = days.firstKey();
        var last = days.lastKey();
        return new Report(games, people(entries), Timeline.build(days, covers), count(entries),
                games.stream().mapToLong(Game::minutes).sum(), days.size(),
                Formats.date(first), Formats.date(last), Formats.period(Period.between(first, last)));
    }

    private static List<Person> people(List<Entry> entries) {
        var histories = new HashMap<String, List<Participation>>();
        entries.stream().sorted(Comparator.comparing(Entry::date).reversed().thenComparing(Entry::title))
                .forEach(play -> play.winsByPlayer().forEach((name, won) -> histories
                        .computeIfAbsent(name, key -> new ArrayList<>()).add(new Participation(play, won))));
        return histories.entrySet().stream().map(group -> {
            var history = group.getValue();
            long rounds = history.stream().mapToLong(p -> p.play().quantity()).sum();
            return new Person(group.getKey(), List.copyOf(history), rounds);
        }).sorted(Comparator.comparingLong(Person::rounds).reversed().thenComparing(Person::name)).toList();
    }

    static long count(List<Entry> entries) {
        return entries.stream().mapToLong(Entry::quantity).sum();
    }

    public long totalGameDays() {
        return games.stream().mapToLong(Game::playedDays).sum();
    }

    public String duration() {
        return Formats.duration(minutes);
    }
}
