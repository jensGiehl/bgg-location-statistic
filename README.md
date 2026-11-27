# Rundenbuch

Rundenbuch erzeugt aus den BoardGameGeek-Plays einer Spielrunde eine mobile Statistikseite. Die Spring-Boot-Anwendung schreibt eine UTF-8-Datei nach `target/pages/index.html` und beendet sich. Die Datei lässt sich direkt im Browser öffnen. Bootstrap aus dem WebJar und das Diagramm sind in die HTML-Datei eingebettet. Spielcover lädt der Browser von BoardGameGeek.

## Auf GitHub Pages veröffentlichen

1. Unter **Settings → Pages → Build and deployment → Source** die Option **GitHub Actions** auswählen.
2. Unter **Settings → Secrets and variables → Actions → Repository secrets** das Secret `BGG_API_KEY` anlegen. Den API-Token erhältst du über deine registrierte Anwendung bei [BoardGameGeek](https://boardgamegeek.com/applications).
3. Das Projekt auf `main` oder `master` pushen. Der Workflow **Rundenbuch veröffentlichen** baut und testet die Anwendung, erzeugt die Seite und veröffentlicht sie. Die URL steht beim erfolgreichen Deployment unter **Environments → github-pages**.

Der Workflow läuft außerdem jeden Freitag um **05:17 UTC** (06:17 Uhr MEZ bzw. 07:17 Uhr MESZ) auf dem Standardbranch. Über **Actions → Rundenbuch veröffentlichen → Run workflow** lässt er sich manuell starten. Ein fehlgeschlagener BGG-Abruf stoppt den Build vor dem Deployment.

| Actions-Secret | Verwendung |
| --- | --- |
| `BGG_API_KEY` | Erforderlicher API-Token für den Generator |
| `BGG_USERNAME` | Optionaler BGG-Benutzer, standardmäßig `JensG` |
| `BGG_LOCATION` | Optionaler Ortsfilter, standardmäßig `Frankenthal (Jochen)` |
| `BGG_CONFIG_JSON` | Optionale Spring-Konfiguration, etwa zusätzliche Anzeigenamen |

Beispiel für den Inhalt von `BGG_CONFIG_JSON`. Die eckigen Klammern erhalten Leerzeichen, Großschreibung und Satzzeichen im Originalnamen.

```json
{"bgg":{"names":{"[Jens G.]":"Jens","[Anna (Spieleclub)]":"Anna"}}}
```

## Lokal bauen und ausführen

Der Generator verwendet JDK 27, Maven 3.9 und Spring Boot 4.2. Der Workflow installiert den BGG-Client aus dem festgelegten Release-Commit. Lokal genügt dieser Schritt einmalig mit JDK 21 als `JAVA_HOME`. [bggClient 1.0.6](https://github.com/jensGiehl/bggClient/tree/706748ad23212c313f46d47944d27f27eab30c50)

```sh
git clone --branch v1.0.6-6 --depth 1 https://github.com/jensGiehl/bggClient.git vendor/bgg-client
mvn -B -ntp -f vendor/bgg-client/pom.xml install -DskipTests -Dmaven.javadoc.skip=true
```

Anschließend JDK 27 für Maven und `java` auswählen. `mvn verify` führt die Integrationstests mit einer lokalen XML-Testquelle aus. Zum Erzeugen der echten Seite muss `BGG_API_KEY` in der Umgebung gesetzt sein.

```sh
mvn -B -ntp verify
java -jar target/bgg-location-overview.jar
```

Alle Einstellungen lassen sich als Spring-Boot-Parameter übergeben. Beispiel für Bash oder PowerShell in einer Zeile:

```sh
java -jar target/bgg-location-overview.jar --bgg.username=JensG "--bgg.location=Frankenthal (Jochen)" "--bgg.names.[Jens G.]=Jens" --bgg.output=target/pages/index.html
```

## Was gezählt wird

- Der Generator liest alle Play-Seiten und vergleicht den Ortsnamen exakt. Der Ort dient ausschließlich als Filter.
- Erster und letzter Spieltag sowie der Abstand in Kalenderjahren, Monaten und Tagen beziehen sich auf die gefilterten Einträge. Mehrere Einträge am selben Datum zählen als ein Spieltag.
- Die Übersicht hebt den ersten und letzten Spieltag sowie die Zeitspanne dazwischen als Kennzahlen hervor.
- Für Partien und Teilnahmen zählt die BGG-Menge `quantity`. Die protokollierte Dauer `length` wird als Gesamtzeit des jeweiligen Eintrags einmal addiert. Fehlende Minuten zählen als null. Spieleliste und Kreisdiagramm zählen je BGG-Spiel-ID die verschiedenen Spieltage. Wurden an einem Datum mehrere Spiele gespielt, zählt das Datum im Diagramm einmal je Spiel. Die Mitte zeigt die Summe dieser Spieltage.
- Die Standardnamen sind **Jochen** für `Jochen (Frankenthal)`, **Torben** für `Torben (Nachbar von Jochen)`, **Georg** für `Georg (Freundeskreis Jochen)` und **Tim** für `Tim (Freundeskreis Jochen)`. Weitere Mappings ergänzen diese Vorgaben oder überschreiben einzelne Namen. Ansonsten bleibt der erfasste Name erhalten. Bei leerem Namensfeld dient der BGG-Benutzername als Ersatz. Gleiche Anzeigenamen zählen innerhalb eines Eintrags einmal.
- Ein Klick auf einen Namen öffnet die persönlichen Partien, neueste zuerst. Der Spieltitel führt zum jeweiligen Partieprotokoll auf BoardGameGeek. Neben Datum und Dauer erscheint ein 🏆, wenn für diese Person ein Sieg eingetragen ist. Bei einem Sammeleintrag stehen die Anzahl der Partien und deren Gesamtdauer zusammen.
- Das Monatsraster zeigt die Anzahl verschiedener Spieltage. Es enthält alle Jahre zwischen dem ersten und letzten Spieltag, auch vollständig leere Jahre. Ein Klick auf eine Jahreszeile öffnet die datierte Spieleliste mit Covern und Partienzahl.
