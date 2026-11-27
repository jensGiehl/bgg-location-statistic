package de.agiehl;

import de.agiehl.bgg.BggClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(ReportSettings.class)
public class RundenbuchApplication {
    public static void main(String[] args) {
        SpringApplication.run(RundenbuchApplication.class, args).close();
    }

    @Bean
    BggClient bggClient(ReportSettings settings) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new IllegalArgumentException("BGG_API_KEY fehlt.");
        }
        return BggClient.of(settings.apiKey());
    }

    @Bean
    CommandLineRunner generate(ReportSettings settings, BggClient client) {
        return args -> {
            var source = new PlaySource(client);
            var report = source.load(settings);
            new ReportWriter().write(report, settings.output());
        };
    }
}
