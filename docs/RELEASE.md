# Release erstellen

Diese Anleitung beschreibt, wie ein signiertes Release-APK von Forkout entsteht.

## 1. Signierschlüssel anlegen (einmalig)

Android installiert Updates nur, wenn sie mit **demselben Schlüssel** signiert sind wie die erste Version. Den Schlüssel deshalb **sicher aufbewahren** (z. B. im Passwort-Manager und als Backup außerhalb des Rechners). Geht er verloren, können Nutzer:innen kein Update installieren, sondern müssten die App neu installieren.

Den Schlüssel **außerhalb des Repositorys** anlegen, zum Beispiel direkt neben dem Projektordner:

```bash
keytool -genkeypair -v -keystore ../forkout-release.jks -alias forkout -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` liegt bei Android Studio unter `jbr/bin/`. Das Programm fragt nach zwei Passwörtern und ein paar Angaben zur Person. Ein Pseudonym genügt dort.

## 2. `keystore.properties` anlegen

`keystore.properties.example` nach `keystore.properties` kopieren und die eigenen Werte eintragen. Die Datei ist in `.gitignore` eingetragen und darf **nie** committet werden, ebenso wenig `*.jks`- oder `*.keystore`-Dateien.

## 3. Version erhöhen

In `app/build.gradle.kts`:

- `versionCode` um 1 erhöhen (muss bei jedem Release steigen)
- `versionName` nach semantischer Versionierung setzen, z. B. `0.1.0`

Danach `CHANGELOG.md` ergänzen.

## 4. Bauen und prüfen

```bash
./gradlew test lintDebug assembleRelease
```

Das signierte APK liegt danach unter `app/build/outputs/apk/release/app-release.apk`. Ohne `keystore.properties` entsteht stattdessen `app-release-unsigned.apk`, das sich nicht installieren lässt.

Vor dem Veröffentlichen das Release-APK auf einem echten Gerät installieren und kurz durchklicken: Plan anlegen, Training starten und beenden, Export und Import.

## 5. Veröffentlichen

1. Commit mit Versionsänderung und Changelog auf `main` pushen und die CI abwarten.
2. Tag setzen, z. B. `v0.1.0`, und pushen.
3. Auf GitHub unter *Releases → Draft a new release* den Tag wählen, den Changelog-Abschnitt als Beschreibung einfügen und `app-release.apk` anhängen (am besten umbenannt in `forkout-0.1.0.apk`).
