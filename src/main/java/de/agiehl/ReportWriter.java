package de.agiehl;

import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class ReportWriter {
    void write(Report report, Path output) throws IOException {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        var context = new Context(Locale.GERMAN);
        context.setVariable("report", report);
        context.setVariable("slices", PieChart.slices(report));
        context.setVariable("bootstrap", resource("META-INF/resources/webjars/bootstrap/5.3.8/css/bootstrap.min.css"));
        context.setVariable("theme", resource("report.css"));
        var directory = Files.createDirectories(output.toAbsolutePath().getParent());
        var pending = Files.createTempFile(directory, "rundenbuch-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(pending, StandardCharsets.UTF_8)) {
                engine.process("report.html", context, writer);
            }
            try {
                Files.move(pending, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(pending, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private static String resource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }
}
