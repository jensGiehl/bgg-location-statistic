package de.agiehl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.agiehl.bgg.BggClient;
import de.agiehl.bgg.config.BggClientConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.support.GenericApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GenerationTest {
    @TempDir Path directory;

    @Test
    void generatesFromAllPagesUsingSpringSettings() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var pages = new AtomicInteger();
        server.createContext("/plays", exchange -> {
            var query = exchange.getRequestURI().getQuery();
            if (!query.contains("username=JensG") || !"Bearer test-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }
            pages.incrementAndGet();
            var plays = query.contains("page=1") ? """
                    <play date="2022-06-05" quantity="2" length="1450" location="Frankenthal (Jochen)">
                      <item objectid="13" name="CATAN"/><players><player name="Jochen (Frankenthal)"/>
                      <player name="Jochen"/><player name="Torben (Nachbar von Jochen)"/></players></play>
                    <play date="2022-06-05" quantity="1" length="50" location="Frankenthal (Jochen)">
                      <item objectid="9209" name="Zug um Zug"/><players><player name="Anna &amp; Co."/></players></play>
                    <play date="1998-12-31" quantity="9" length="100" location="Anderswo">
                      <item objectid="999" name="Ausgefiltert"/></play>
                    """ : query.contains("page=2") ? """
                    <play date="2019-04-01" quantity="1" length="63" location="Frankenthal (Jochen)">
                      <item objectid="13" name="CATAN"/><players><player name="Jens G."/><player username="Lea"/></players></play>
                    """ : "";
            xml(exchange, "<plays>" + plays + "</plays>");
        });
        server.createContext("/thing", exchange -> xml(exchange, """
                <items><item type="boardgame" id="13"><image>https://cf.geekdo-images.com/W3Bsga_uLP9kO91gZ7H8yw__medium/img/GgwxbZQYfLsZoj_rjx0SAhLVzcE=/fit-in/500x500/filters:no_upscale():strip_icc()/pic2419375.jpg</image></item>
                <item type="boardgame" id="9209"><thumbnail>https://cf.geekdo-images.com/ZWJg0dCdrWHxVnc0eFXK8w__listitem/img/hhKjqlG2YPBrSOde9GUfMar11bc=/fit-in/175x175/filters:strip_icc()/pic38668.jpg</thumbnail></item></items>
                """));
        server.start();
        try {
            var config = BggClientConfig.builder().apiKey("test-token")
                    .baseUri(URI.create("http://127.0.0.1:" + server.getAddress().getPort())).build();
            var page = directory.resolve("index.html");
            try (var context = new SpringApplicationBuilder(RundenbuchApplication.class)
                    .initializers(application -> ((GenericApplicationContext) application).registerBean("fixtureClient",
                            BggClient.class, () -> BggClient.builder().config(config).build(), bean -> bean.setPrimary(true)))
                    .run("--bgg.output=" + page, "--bgg.api-key=test-token", "--bgg.names.[Jens G.]=Jens")) {
                assertEquals("JensG", context.getBean(ReportSettings.class).username());
            }
            assertEquals(3, pages.get());
            String html = Files.readString(page);
            for (var value : List.of("01.04.2019", "05.06.2022", "3 Jahre, 2 Monate und 4 Tage",
                    "1 Tag, 2 Stunden und 3 Minuten", ">2020<", ">2021<", ">Jochen<", ">Torben<",
                    ">Jens<", "Anna &amp; Co.", ">Lea<", ">4</strong>", ">2</strong>", "url(#slice-0)", "<image")) {
                assertTrue(html.contains(value), value);
            }
            for (var value : List.of("Frankenthal", "Nachbar von", "Ausgefiltert", "test-token", "th:text")) {
                assertFalse(html.contains(value), value);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rendersAnEmptySelectionAndSingleGameChart() throws IOException {
        var output = directory.resolve("empty.html");
        new ReportWriter().write(Report.from(List.of(), java.util.Map.of()), output);
        assertTrue(Files.readString(output).contains("keine Partien eingetragen"));
        var entry = new Report.Entry(java.time.LocalDate.parse("2026-01-01"), 13, "CATAN", 3, 0, List.of("Jochen"));
        var report = Report.from(List.of(entry), java.util.Map.of());
        assertEquals(3, report.people().getFirst().rounds());
        assertEquals("0 Minuten", report.duration());
        assertEquals("0 Jahre, 0 Monate und 0 Tage", report.span());
        assertEquals(200, PieChart.slices(report).getFirst().width(), 0.0001);
    }

    private static void xml(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (exchange) {
            exchange.getResponseBody().write(bytes);
        }
    }
}
