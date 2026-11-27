package de.agiehl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PieChart {
    private static final List<String> COLORS = List.of("#39846e", "#dba444", "#bc604b", "#7489b7", "#a68ca7", "#69844c");
    public record Slice(Report.Game game, String path, String color, double x, double y, double width, double height) {}

    private PieChart() {}

    static List<Slice> slices(Report report) {
        var result = new ArrayList<Slice>();
        long totalGameDays = report.totalGameDays();
        long previous = 0;
        for (var game : report.games()) {
            double start = 2 * Math.PI * previous / totalGameDays - Math.PI / 2;
            previous += game.playedDays();
            double end = 2 * Math.PI * previous / totalGameDays - Math.PI / 2;
            String path = game.playedDays() == totalGameDays
                    ? "M100 0a100 100 0 1 1 0 200a100 100 0 1 1 0-200z"
                    : String.format(Locale.ROOT, "M100 100 L%.5f %.5f A100 100 0 %d 1 %.5f %.5f Z",
                            x(start), y(start), end - start > Math.PI ? 1 : 0, x(end), y(end));
            double left = Math.min(100, Math.min(x(start), x(end)));
            double right = Math.max(100, Math.max(x(start), x(end)));
            double top = Math.min(100, Math.min(y(start), y(end)));
            double bottom = Math.max(100, Math.max(y(start), y(end)));
            for (int quarter = -1; quarter <= 3; quarter++) {
                double angle = quarter * Math.PI / 2;
                if (start <= angle && angle <= end) {
                    left = Math.min(left, x(angle));
                    right = Math.max(right, x(angle));
                    top = Math.min(top, y(angle));
                    bottom = Math.max(bottom, y(angle));
                }
            }
            result.add(new Slice(game, path, COLORS.get(result.size() % COLORS.size()), left, top, right-left, bottom-top));
        }
        return result;
    }

    private static double x(double angle) {
        return 100 + 100 * Math.cos(angle);
    }

    private static double y(double angle) {
        return 100 + 100 * Math.sin(angle);
    }
}
