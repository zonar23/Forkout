# Forkout – Format der Datensicherung

Forkout exportiert alle Daten als eine JSON-Datei (UTF-8). Das Format ist offen, damit du deine Daten jederzeit lesen, weiterverarbeiten oder in andere Werkzeuge übernehmen kannst.

Export und Import findest du in der App unter **Einstellungen → Daten**. Der Import **ersetzt alle aktuellen Daten** in der App. Er läuft in einer einzigen Datenbank-Transaktion: Ist die Datei fehlerhaft, bleiben die bisherigen Daten unverändert.

## Grundregeln

- **Gewichte** sind ganze Zahlen in **Gramm** (`82500` = 82,5 kg).
- **Zeitpunkte** sind ISO-8601-Zeitstempel in UTC, z. B. `"2026-10-24T09:00:00Z"`.
- **Reihenfolgen** (Einheiten, Übungen in einer Einheit, Sätze) ergeben sich aus der Reihenfolge in den Listen.
- **IDs** verknüpfen Einträge nur innerhalb einer Datei: `exerciseId` verweist auf `exercises[].id`, `planDayId` auf `plans[].days[].id`.
- Unbekannte Felder werden beim Import ignoriert. Fehlende optionale Felder bekommen ihren Standardwert.

## Aufbau

```json
{
  "formatVersion": 1,
  "app": "forkout",
  "exportedAt": "2026-10-24T09:00:00Z",
  "settings": { "amoledBlack": false, "defaultRestSeconds": 90, "weightUnit": "KG" },
  "exercises": [
    {
      "id": 4,
      "name": "Beinpresse",
      "type": "MACHINE",
      "muscleGroups": ["LEGS", "GLUTES"],
      "setupNote": "Sitz Stufe 4",
      "isBuiltIn": true,
      "isArchived": false
    }
  ],
  "plans": [
    {
      "id": 1,
      "name": "Coach-Plan Herbst 2026",
      "goal": "Muskelaufbau",
      "sessionsPerWeek": 3,
      "note": "",
      "createdAt": "2026-10-20T08:00:00Z",
      "isArchived": false,
      "days": [
        {
          "id": 1,
          "name": "Einheit A",
          "exercises": [
            {
              "exerciseId": 4,
              "note": "",
              "sets": [
                { "targetReps": 12, "targetWeightGrams": 82500, "restSeconds": 90 }
              ]
            }
          ]
        }
      ]
    }
  ],
  "workouts": [
    {
      "planDayId": 1,
      "planDayName": "Einheit A",
      "startedAt": "2026-10-22T17:00:00Z",
      "finishedAt": "2026-10-22T17:52:00Z",
      "note": "",
      "sets": [
        {
          "exerciseId": 4,
          "exercisePosition": 0,
          "setIndex": 0,
          "targetReps": 12,
          "targetWeightGrams": 82500,
          "restSeconds": 90,
          "actualReps": 11,
          "actualWeightGrams": 85000,
          "rir": null,
          "completedAt": "2026-10-22T17:04:10Z"
        }
      ]
    }
  ]
}
```

## Felder

| Feld | Bedeutung |
|---|---|
| `formatVersion` | Version dieses Formats. Neuere Versionen kann eine ältere App nicht importieren. |
| `exercises[].type` | `MACHINE`, `CABLE`, `FREE_WEIGHT` oder `BODYWEIGHT` |
| `exercises[].muscleGroups` | Liste aus `CHEST`, `BACK`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `CORE`, `LEGS`, `HAMSTRINGS`, `GLUTES`, `CALVES`. Unbekannte Werte werden übersprungen. |
| `exercises[].isBuiltIn` | Mitgelieferte Basis-Übung |
| `workouts[].planDayId` | Einheit, aus der das Training gestartet wurde. `null`, wenn sie nicht mehr existiert. |
| `workouts[].planDayName` | Kopie des Einheitennamens, damit der Verlauf lesbar bleibt |
| `workouts[].finishedAt` | `null` bei einem noch laufenden Training |
| `workouts[].sets[].target*` | Soll-Werte, beim Start aus dem Plan kopiert |
| `workouts[].sets[].actual*` | Ist-Werte |
| `workouts[].sets[].completedAt` | `null` bei einem nicht abgehakten Satz |
| `settings.weightUnit` | `KG` (`LB` ist vorbereitet) |
