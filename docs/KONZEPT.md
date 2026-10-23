# Forkout – Konzept

> **Fork it. Lift it.**
> Eine freie, werbefreie Open-Source-App für Android, mit der man Trainingspläne anlegt und im Gym wie eine Checkliste abarbeitet.

Stand: Oktober 2026 · Lizenz: MIT · Plattform: Android

---

## 1. Vision

Forkout ist ein schlanker, moderner Trainingsbegleiter für Kraftsport im Studio. Die App ersetzt den Papier-Trainingsplan: Übungen, Sätze, Wiederholungen und Gewichte stehen am Handy, werden beim Training abgehakt und landen automatisch im Verlauf.

### Leitprinzipien

1. **Offline-first & privat:** kein Konto, keine Cloud-Pflicht, keine Tracker, keine Werbung. Alle Daten bleiben auf dem Gerät.
2. **Im Gym schnell bedienbar:** große Touch-Flächen, wenige Taps pro Satz, Bildschirm bleibt an.
3. **Die eigenen Daten gehören dem Nutzer:** jederzeit Export und Import als offene JSON-Datei.
4. **Erweiterbar:** saubere Architektur, damit neue Funktionen und Beiträge aus der Community leicht andocken.
5. **Open Source:** Code unter MIT-Lizenz, offene Entwicklung auf GitHub.

### Zielgruppe

- Studio-Trainierende mit festem Plan vom Coach (Maschinen, Kabelzug, freie Gewichte)
- Menschen, die ihren Fortschritt sehen wollen, ohne eine Abo-App zu nutzen
- Datenschutzbewusste Nutzer:innen (F-Droid-Publikum)

---

## 2. Funktionsumfang Version 1 (MVP)

### 2.1 Übungsbibliothek
- Liste aller Übungen, durchsuchbar und filterbar nach Typ und Muskelgruppe
- Felder pro Übung:
  - **Name**
  - **Typ:** Maschine · Kabelzug · Freie Gewichte · Körpergewicht
  - **Muskelgruppen** (Mehrfachauswahl)
  - **Geräte-Einstellung / Notiz**, z. B. „Sitz Stufe 4“ oder „Blöcke unter 16“. Wird im Training prominent angezeigt.
- Mitgelieferte Basis-Übungen mit **generischen Namen** (siehe Abschnitt 7), eigene Übungen anlegbar
- Übungen archivieren statt löschen, damit der Verlauf erhalten bleibt

### 2.2 Pläne & Einheiten
- Mehrere Pläne, z. B. „Coach-Plan Herbst 2026“
- Planfelder: Name, Ziel (z. B. Muskelaufbau), Einheiten pro Woche, Notiz
- Ein Plan enthält **Einheiten** (A, B, C …)
- Eine Einheit enthält **Übungen** aus der Bibliothek in fester Reihenfolge (per Drag & Drop sortierbar)
- Pro Übung: **Sätze** mit Soll-Wiederholungen, Soll-Gewicht und Pause
- Komfort: „Satz duplizieren“, „Werte auf alle Sätze übernehmen“, Plan oder Einheit kopieren

### 2.3 Trainingsmodus (Kernfunktion)
- Einheit starten → Checkliste aller Übungen und Sätze
- Jeder Satz ist mit den **Soll-Werten vorbelegt** und wird mit einem Tap abgehakt
- Ist-Werte (Wdh., Gewicht) lassen sich vor dem Abhaken schnell anpassen (± Buttons, Stepper)
- Anzeige der **Werte vom letzten Training** dieser Übung
- **Pausentimer** startet automatisch nach dem Abhaken, mit Vibration am Ende
  - Timer basiert auf einem Endzeitpunkt und nicht auf einem Zähler, damit er nach Bildschirmsperre korrekt bleibt
- Bildschirm bleibt während des Trainings an
- Training abbrechen, pausieren oder fortsetzen (Zustand bleibt beim App-Wechsel erhalten)
- Abschluss-Zusammenfassung: Dauer, erledigte Sätze, Gesamtvolumen (kg × Wdh.)

### 2.4 Verlauf
- Liste vergangener Trainings mit Datum, Einheit und Dauer
- Detailansicht eines Trainings
- Pro Übung: Verlauf der Gewichte und Wiederholungen (in V1 als Liste, Diagramme folgen später)

### 2.5 Startseite „Heute“
- Nächste Einheit mit Start-Button
- **Wochenfortschritt**, z. B. „2 / 3 Einheiten diese Woche“
- Letztes Training auf einen Blick

### 2.6 Einstellungen & Daten
- **Export / Import** aller Daten als JSON (über den Android-Dateidialog, keine Speicherberechtigung nötig)
- Gewichtseinheit kg (lb später)
- AMOLED-Schwarz an/aus
- Standard-Pausendauer

---

## 3. Roadmap (nach V1)

| Version | Funktion |
|---|---|
| 0.2 | Diagramme pro Übung (Gewicht, Volumen, geschätztes 1RM) |
| 0.2 | **Progressionshinweis** (doppelte Progression: alle Sätze mit Ziel-Wdh. geschafft → „Gewicht erhöhen?“) |
| 0.2 | Optionales **RIR-Feld** (Wiederholungen in Reserve) pro Satz |
| 0.3 | Pausentimer als Benachrichtigung bei gesperrtem Bildschirm (Foreground Service) |
| 0.3 | Supersätze / Zirkel |
| 0.3 | Pläne teilen per Datei oder QR-Code |
| 0.4 | Mehrsprachigkeit (DE/EN, Community-Übersetzungen) |
| 0.4 | Körpergewicht & Maße |
| später | Health Connect, Wear OS, Home-Screen-Widget, Plan-Import per Foto (OCR) |

---

## 4. Screens & Navigation

Untere Navigationsleiste mit vier Bereichen:

1. **Heute:** nächste Einheit, Wochenfortschritt
2. **Pläne:** Planliste → Plan-Detail → Einheiten-Editor
3. **Übungen:** Bibliothek → Übungs-Detail (inkl. Verlauf)
4. **Verlauf:** vergangene Trainings

Zusätzlich:
- **Trainingsmodus:** Vollbild, ohne Navigationsleiste
- **Einstellungen:** über das Zahnrad oben rechts

---

## 5. Design

- **Dunkles Theme als Standard**, optional echtes Schwarz (AMOLED)
- Eine kräftige **Akzentfarbe** (Vorschlag: Lime `#C6F432`) für Haken, Buttons und den Timer
- Farb-Tokens (Vorschlag):
  - Hintergrund `#0E0F12` (AMOLED: `#000000`)
  - Surface `#17191E`, Surface erhöht `#20232A`
  - Text primär `#F2F3F5`, sekundär `#9AA0AA`
  - Akzent `#C6F432`, Text auf Akzent `#0E0F12`
  - Fehler `#FF5C5C`
- Material 3 als Basis, aber **eigene Markenfarben** statt Dynamic Color
- Typografie: groß und klar, Zahlen (Gewicht, Wdh.) besonders prominent mit Tabellenziffern
- Touch-Ziele mindestens 56 dp im Trainingsmodus
- Dezente Animationen: Häkchen, Fortschrittsbalken, Timer-Ring
- Erledigte Sätze werden sichtbar „abgehakt“ (gedimmt, mit Akzent-Haken)

---

## 6. Datenmodell

```
Exercise
  id, name, type (MACHINE | CABLE | FREE_WEIGHT | BODYWEIGHT),
  muscleGroups, setupNote, isBuiltIn, isArchived

Plan
  id, name, goal, sessionsPerWeek, note, createdAt, isArchived

PlanDay                      // "Einheit A"
  id, planId, name, position

PlanExercise
  id, planDayId, exerciseId, position, note

PlanSet
  id, planExerciseId, position, targetReps, targetWeightGrams, restSeconds

WorkoutSession
  id, planDayId (nullable), planDayName (Kopie), startedAt, finishedAt (nullable = läuft), note

WorkoutSet
  id, sessionId, exerciseId, exercisePosition, setIndex,
  targetReps, targetWeightGrams, restSeconds,
  actualReps, actualWeightGrams, rir (nullable), completedAt (nullable = offen)
```

Muskelgruppen sind eine feste Liste (Brust, Rücken, Schultern, Bizeps, Trizeps, Rumpf, Beine, Beinbeuger, Gesäß, Waden) und werden direkt an der Übung gespeichert.

Designentscheidungen:
- **Gewichte als Ganzzahl in Gramm**, um Rundungsfehler bei Kommazahlen (z. B. 7,5 kg) zu vermeiden
- **WorkoutSet speichert Soll und Ist als Kopie**, damit spätere Planänderungen den Verlauf nicht verfälschen
- **WorkoutSession speichert den Namen der Einheit als Kopie**, damit der Verlauf lesbar bleibt, auch wenn der Plan gelöscht wird
- Übungen werden archiviert und nie hart gelöscht, solange es Verlaufseinträge gibt

---

## 7. Mitgelieferte Basis-Übungen (Seed)

Generische deutsche Namen ohne Marken- oder Gerätebezeichnungen eines Herstellers. Die Namen sind Daten in der Datenbank und werden nicht übersetzt:

| Übung | Typ | Muskelgruppen |
|---|---|---|
| Kabelzug Push-Pull (stehend) | Kabelzug | Brust, Rücken, Rumpf |
| Latzug | Maschine | Rücken, Bizeps |
| Rumpfrotation | Maschine | Rumpf |
| Beinpresse | Maschine | Beine, Gesäß |
| Beinbeuger | Maschine | Beinbeuger |
| Brustpresse | Maschine | Brust, Trizeps, Schulter |
| Kabelzug tief mit Rotation | Kabelzug | Rumpf, Rücken |
| Bauchtrainer | Maschine | Rumpf |

Erweiterbar um gängige Grundübungen (Kniebeuge, Kreuzheben, Bankdrücken, Rudern, Schulterdrücken …).

Persönliche Pläne (z. B. vom eigenen Coach) gehören **nicht ins Repository**. Sie werden in der App angelegt oder per JSON importiert.

---

## 8. Technik

| Bereich | Wahl |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architektur | MVVM, Unidirectional Data Flow (StateFlow) |
| Datenbank | Room |
| Einstellungen | DataStore |
| Navigation | Navigation Compose (typsichere Routen) |
| Dependency Injection | Hilt |
| Serialisierung | kotlinx.serialization (Export/Import) |
| Build | Gradle Kotlin DSL + Version Catalog |
| Min. Android | API 26 (Android 8.0) |
| Tests | JUnit, Room-Tests, Compose-UI-Tests für Kernabläufe |
| CI | GitHub Actions (Build + Tests bei jedem Pull Request) |

Keine Internet-Berechtigung in V1. Das ist ein klares Datenschutz-Signal.

---

## 9. Open-Source-Organisation

- Repository auf GitHub, Lizenz **MIT**
- Dateien im Repo: `README.md`, `LICENSE`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `CHANGELOG.md`, Issue-Vorlagen
- **Keine** Fotos aus Studio- oder Coach-Plänen, keine Herstellermarken in Seed-Daten oder Grafiken. Icons und Grafiken sind selbst erstellt oder frei lizenziert.
- Releases als signiertes APK über GitHub Releases, später **F-Droid**, optional Play Store
- Semantische Versionierung (`0.1.0` …)

---

## 10. Meilensteine bis Version 0.1

| # | Meilenstein | Ergebnis |
|---|---|---|
| M0 | Projekt-Setup | Android-Studio-Projekt, Theme, Navigation-Grundgerüst, CI |
| M1 | Datenschicht | Room-Entities, DAOs, Repositories, Seed-Übungen, Tests |
| M2 | Übungsbibliothek | Liste, Suche, Anlegen und Bearbeiten |
| M3 | Plan-Editor | Pläne, Einheiten, Übungen und Sätze anlegen und sortieren |
| M4 | Trainingsmodus | Checkliste, Ist-Werte, Pausentimer, Abschluss |
| M5 | Verlauf & Heute | Trainingsliste, Details, Wochenfortschritt |
| M6 | Export / Import | JSON-Backup und -Wiederherstellung |
| M7 | Release 0.1 | README, Screenshots, signiertes APK |
