package de.agiehl;

import de.agiehl.bgg.BggClient;
import de.agiehl.bgg.model.plays.Play;
import de.agiehl.bgg.request.PlaysRequest;
import de.agiehl.bgg.request.ThingRequest;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Gatherers;

final class PlaySource {
    private final BggClient client;

    PlaySource(BggClient client) {
        this.client = client;
    }

    Report load(ReportSettings settings) {
        var request = PlaysRequest.builder().username(settings.username()).build();
        var entries = client.plays().items(request)
                .filter(play -> settings.location().equals(play.getLocation()))
                .map(play -> entry(play, settings.names()))
                .toList();
        return Report.from(entries, covers(entries));
    }

    private static Report.Entry entry(Play play, Map<String, String> aliases) {
        var players = play.getPlayers().stream()
                .map(p -> p.getName() == null || p.getName().isBlank() ? p.getUsername() : p.getName())
                .filter(name -> name != null && !name.isBlank())
                .map(name -> aliases.getOrDefault(name, name))
                .distinct().toList();
        var game = play.getItem();
        int quantity = Objects.requireNonNullElse(play.getQuantity(), 1);
        int minutes = Objects.requireNonNullElse(play.getLength(), 0);
        return new Report.Entry(LocalDate.parse(play.getDate()), game.getObjectid(), game.getName(),
                quantity, minutes, players);
    }

    private Map<Integer, String> covers(List<Report.Entry> entries) {
        var covers = new HashMap<Integer, String>();
        entries.stream().map(Report.Entry::gameId).distinct().gather(Gatherers.windowFixed(20))
                .map(ids -> client.things().fetch(ThingRequest.builder().ids(ids).build()))
                .flatMap(response -> response.getItems().stream())
                .forEach(game -> {
                    var image = Objects.toString(game.getThumbnail(), "");
                    if (image.isBlank()) {
                        image = Objects.toString(game.getImage(), "");
                    }
                    if (isHttpsImage(image)) {
                        covers.put(game.getId(), image);
                    }
                });
        return covers;
    }

    private static boolean isHttpsImage(String value) {
        try {
            var uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
