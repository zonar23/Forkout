# Mitmachen bei Forkout

Danke, dass du Forkout verbessern möchtest! Jeder Beitrag zählt: Fehlermeldungen, Ideen, Übersetzungen und Code.

## Fehler und Ideen

Bitte ein [Issue](https://github.com/zonar23/Forkout/issues) anlegen. Bei Fehlern hilft:

- was du getan hast, was passiert ist und was du erwartet hast
- Android-Version, Gerät und Forkout-Version (unter *Android-Einstellungen → Apps → Forkout*)
- wenn möglich ein Screenshot, ohne persönliche Daten

## Code beitragen

1. Repository forken und einen Branch anlegen.
2. In kleinen, überprüfbaren Schritten arbeiten. Vor dem Pull Request müssen diese Befehle fehlerfrei durchlaufen:
   ```bash
   ./gradlew assembleDebug test lintDebug
   ```
3. Pull Request mit kurzer Beschreibung öffnen. Die CI prüft Build, Tests und Lint automatisch.

Bei größeren Änderungen am besten vorher ein Issue eröffnen, damit wir die Richtung abstimmen können.

## Regeln

Die Konventionen stehen ausführlich in [CLAUDE.md](CLAUDE.md). Die wichtigsten:

- Code, Bezeichner und Kommentare auf **Englisch**, UI-Texte ausschließlich in `strings.xml`
- Gewichte als `Int` in Gramm, Umrechnung nur in der UI
- Datenbankänderungen nur mit Migration und exportiertem Schema, dazu Tests
- **Keine** Internet-Berechtigung, Analytics, Tracker oder proprietären SDKs
- Neue Abhängigkeiten nur mit MIT-kompatibler Lizenz
- Keine Herstellermarken, keine fremden Fotos, keine persönlichen Trainingspläne im Repository

## Lizenz

Mit deinem Beitrag stimmst du zu, dass er unter der [MIT-Lizenz](LICENSE) des Projekts veröffentlicht wird.
