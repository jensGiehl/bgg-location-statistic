package de.agiehl;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Map;

@ConfigurationProperties("bgg")
public record ReportSettings(String username, String location, Map<String, String> names, Path output, String apiKey) {
    public ReportSettings {
        if (username == null || username.isBlank() || location == null || location.isBlank()) {
            throw new IllegalArgumentException("BGG-Benutzer und Ort müssen angegeben sein.");
        }
        names = Map.copyOf(names);
    }

    @Override
    public String toString() {
        return "ReportSettings[output=" + output + "]";
    }
}
