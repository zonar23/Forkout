# CLAUDE.md – Projektleitfaden für Forkout

Diese Datei beschreibt das Projekt für KI-Assistenten (Claude Code) und neue Mitwirkende.
Das ausführliche Produktkonzept steht in `docs/KONZEPT.md`. **Vor größeren Änderungen dort nachlesen.**

## Projekt in einem Satz
Forkout ist eine offline-first Open-Source-App für Android (MIT), mit der man Trainingspläne anlegt und im Gym als Checkliste abarbeitet: Sätze abhaken, Gewichte und Wiederholungen protokollieren, Pausentimer, Verlauf.

## Tech-Stack
- Kotlin, Jetpack Compose, Material 3
- MVVM mit `StateFlow`, Unidirectional Data Flow
- Room (Datenbank), DataStore (Einstellungen)
- Hilt (DI), Navigation Compose mit typsicheren Routen
- kotlinx.serialization für JSON-Export/-Import
- Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`)
- minSdk 26, targetSdk = aktuelles stabiles SDK

## Paketstruktur
```
app/src/main/java/<package>/
  data/
    db/          // Room: Database, Entities, DAOs, Migrations
    repository/  // Repositories (einzige Schnittstelle zur UI-Schicht)
    backup/      // JSON-Export/-Import
    seed/        // mitgelieferte Basis-Übungen
  ui/
    theme/       // Farben, Typografie, Shapes (Forkout-Theme)
    components/  // wiederverwendbare Composables
    today/  plans/  exercises/  workout/  history/  settings/
                 // je Feature: Screen + ViewModel + UiState
  di/            // Hilt-Module
```
Paketname: `io.github.<github-user>.forkout` (Platzhalter, beim Setup festlegen).

## Konventionen
- **Code, Bezeichner und Kommentare auf Englisch**, damit das Projekt international beitragsfähig ist. Die Dokumentation für Nutzer darf deutsch sein.
- **Alle UI-Texte in `strings.xml`**, niemals hartcodiert (Standard: Deutsch in `values/`, Englisch in `values-en/` vorbereiten).
- Jeder Screen hat ein `UiState`-Data-Class und ein ViewModel. Composables sind möglichst zustandslos (State Hoisting).
- **Gewichte als `Int` in Gramm** speichern, Umrechnung nur in der UI.
- Verlaufsdaten (`WorkoutSet`) speichern Soll- und Ist-Werte als Kopie. Niemals per Fremdschlüssel auf Plan-Sätze verlassen.
- Übungen mit Verlauf werden archiviert, nicht gelöscht.
- Room-Schemaänderungen **immer mit Migration** und exportiertem Schema (`room.schemaLocation`). Keine `fallbackToDestructiveMigration` in Release-Builds.
- Keine Internet-Berechtigung, keine Analytics, keine Tracker oder proprietären SDKs.
- Neue Abhängigkeiten nur, wenn nötig, und mit MIT-kompatibler Lizenz.

## Design-Regeln
- Dunkles Theme als Standard, optionales AMOLED-Schwarz. Keine Dynamic Color (Markenfarben stehen fest).
- Farb-Tokens (in `ui/theme/Color.kt`):
  - Background `#0E0F12` / AMOLED `#000000`
  - Surface `#17191E`, SurfaceHigh `#20232A`
  - OnSurface `#F2F3F5`, OnSurfaceVariant `#9AA0AA`
  - Primary (Akzent) `#C6F432`, OnPrimary `#0E0F12`
  - Error `#FF5C5C`
- Zahlen (Gewicht, Wdh., Timer) groß und mit Tabellenziffern (`fontFeatureSettings = "tnum"`)
- Touch-Ziele im Trainingsmodus mindestens 56 dp
- Im Trainingsmodus `keepScreenOn` aktiv

## Inhalt & Rechte
- **Keine Herstellermarken** (z. B. Gerätehersteller-Namen) in Seed-Daten, Grafiken oder Texten
- Keine fremden Fotos. Icons und Grafiken sind selbst erstellt oder frei lizenziert (Material Symbols ist ok).
- Persönliche Trainingspläne gehören nie ins Repository

## Arbeitsweise mit Claude
- In kleinen, überprüfbaren Schritten arbeiten. Jeder Schritt muss bauen (`./gradlew assembleDebug`).
- Nach Änderungen an der Datenschicht: Unit-/Room-Tests ergänzen und ausführen (`./gradlew test`).
- Bei Unklarheiten zu Funktionen zuerst `docs/KONZEPT.md` lesen, dann nachfragen statt raten.
- Meilensteine siehe `docs/KONZEPT.md`, Abschnitt 10. Aktuellen Stand unten pflegen.

## Aktueller Stand
- [ ] M0 Projekt-Setup
- [ ] M1 Datenschicht
- [ ] M2 Übungsbibliothek
- [ ] M3 Plan-Editor
- [ ] M4 Trainingsmodus
- [ ] M5 Verlauf & Heute
- [ ] M6 Export / Import
- [ ] M7 Release 0.1
