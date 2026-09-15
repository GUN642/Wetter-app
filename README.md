# Schmuddelwetter

Eine private Android-Wetter-App im Stil des Nothing-OS-Designs (Graphit/Schwarz/Hell,
Dot-Matrix-Zahlen) – mit klassischer Wettervorhersage **und** einem eigenen
Flugwetter-Bereich für Piloten (METAR/TAF, Sicht, Wolkenuntergrenze, Wettererscheinungen,
Flugkategorie).

Reine Privatnutzung: kein Play-Store-Release, kein Tracking, kein Cloud-Konto.

## Features

- **Wetter (Home):** aktuelle Bedingungen, stündlicher Verlauf, 7-Tage-Übersicht –
  Datenquelle: **Deutscher Wetterdienst (DWD)**, MOSMIX Open Data (kostenlos, ohne
  API-Key). Die aktuelle Position wird per GPS/Netzwerk-Standort ermittelt, die
  nächstgelegene DWD-Station automatisch bestimmt.
- **Flugwetter:** METAR/TAF-Abfrage für jeden ICAO-Flugplatz weltweit – Datenquelle:
  **aviationweather.gov** (NOAA, kostenlos, ohne API-Key). Dekodiert werden u. a.:
  - Sichtweite (Meter/Statute Miles inkl. Bruchwerte)
  - Wolkenuntergrenze (tiefste BKN/OVC-Schicht bzw. vertikale Sicht bei Vernebelung)
  - Wettererscheinungen (Regen, Schnee, Gewitter, Nebel, Sandsturm, …) auf Deutsch
  - Wind (Richtung, Stärke, Böen), Temperatur/Taupunkt, QNH
  - Flugkategorie (VFR/MVFR/IFR/LIFR) nach Standard-Sicht-/Untergrenzen-Schwellen
  - TAF-Änderungsgruppen (BECMG/TEMPO/PROB/FM) einzeln aufgeschlüsselt
  - Rohtext (METAR/TAF) wird immer zusätzlich angezeigt
- **Design:** Graphit/Schwarz/Hell-Palette, Dot-Matrix-Schrift ("Silkscreen") für große
  Zahlen, "Space Mono" für Fließtext/Labels – angelehnt an die Nothing-OS-Optik.
- Standort- und Flughafen-Historie werden nur lokal auf dem Gerät gespeichert
  (Jetpack DataStore), keine Cloud-Synchronisation.

## Architektur

```
app/src/main/java/de/privat/schmuddelwetter/
├── data/
│   ├── dwd/        MOSMIX-Streaming-KML-Parser (Stationssuche inklusive), Wettercode-Mapping
│   ├── metar/       METAR/TAF-Textparser, aviationweather.gov-Client, Flughafenliste
│   ├── location/    Standortabfrage (FusedLocationProvider)
│   └── settings/    Lokale Einstellungen (DataStore)
├── di/              Einfacher, handgeschriebener Service-Locator (kein Hilt nötig)
├── ui/
│   ├── theme/       Farben, Typografie, Nothing-Design-Tokens
│   ├── components/  Wiederverwendbare Bausteine (Karten, Divider, Stat-Kacheln, Icons)
│   ├── home/        Wetter-Screen + ViewModel
│   ├── aviation/    Flugwetter-Screen + ViewModel
│   ├── settings/    Einstellungen-Screen + ViewModel
│   └── navigation/  Bottom-Navigation zwischen den drei Bereichen
└── util/            Formatierung, gemeinsame Wetter-Zustands-Enums
```

### Warum DWD MOSMIX statt eines separaten "Aktuelle Wetter"-Feeds?

Die aktuelle Anzeige wird aus dem MOSMIX-Zeitschritt errechnet, der der Ist-Zeit am
nächsten liegt. Das vermeidet einen zweiten, weniger konsistenten DWD-Datensatz
(POI-Beobachtungen, die nur an einem kleineren Stationsnetz existieren) und hält
Home- und Stunden-/Tagesansicht auf derselben Datenbasis.

### Warum wird bei jedem Refresh eine ~36-MB-Datei geladen?

DWD bietet MOSMIX_S inzwischen nur noch als **eine** Sammeldatei für alle ca. 5600
Stationen weltweit an (`all_stations/kml/MOSMIX_S_LATEST_240.kmz`); einen
Einzelstations-Endpunkt oder einen separaten, leichtgewichtigen Stationskatalog
gibt es bei DWD nicht mehr. Entpackt ist die KML-Datei mehrere hundert MB groß –
viel zu groß, um sie komplett im Speicher zu halten. Die App liest sie deshalb in
einem einzigen Streaming-Durchlauf (`MosmixKmlParser.parseNearestStation`):
Koordinaten jeder Station werden sofort mit dem eigenen Standort verglichen, und
nur für die bisher nächstgelegene Station werden die Vorhersagewerte tatsächlich
behalten – alle anderen werden direkt wieder verworfen. Dadurch bleibt der
Speicherbedarf konstant klein, unabhängig von der Dateigröße; der Download selbst
(einmal pro Aktualisierung) bleibt aber ein echter, spürbarer Netzwerk- und
Datenvolumen-Kostenfaktor, den DWDs aktuelles Datenangebot leider vorgibt.

### Warum ein reiner Text-Parser für METAR/TAF statt eines JSON-Schemas?

`aviationweather.gov` liefert auf Wunsch (`format=raw`) die Original-Rohmeldung als
Text. Ein eigener, von der API-Antwortstruktur unabhängiger Parser ist robuster
gegenüber API-Änderungen und zeigt dem Nutzer immer zusätzlich den unveränderten
Rohtext – wichtig für die Luftfahrt, wo die Originalmeldung die verbindliche Quelle
bleibt.

## Build & Ausführen

Dieses Projekt wurde in einer Sandbox mit eingeschränktem Netzwerkzugriff erstellt
(Googles Maven-Repository `dl.google.com` war blockiert) – ein vollständiger
`./gradlew build` konnte hier **nicht** ausgeführt werden. Bitte vor dem ersten
eigenen Build/Test einmal in **Android Studio** öffnen und synchronisieren:

1. Android Studio (aktuelle Version, z. B. Ladybug oder neuer) installieren.
2. Projektordner öffnen → Gradle-Sync abwarten (lädt AGP/AndroidX/Compose-BOM
   herunter, dafür ist normaler Internetzugriff nötig).
3. Auf einem Gerät/Emulator mit **Android 8.0 (API 26)** oder neuer ausführen.
4. Beim ersten Start nach Standortzugriff fragen lassen (für Wetter am aktuellen Ort
   und "nächster Flughafen" im Flugwetter-Tab).

Falls einzelne Material-Icon-Namen in `ui/components/WeatherIcon.kt` in der
tatsächlich aufgelösten `material-icons-extended`-Version abweichen sollten, ist das
eine reine Icon-Zuordnung an einer einzigen Stelle und leicht austauschbar.

### Tests

```
./gradlew testDebugUnitTest
```

Der METAR/TAF-Parser (`MetarTafParserTest`) ist reine Kotlin-/JVM-Logik und ohne
Android-Abhängigkeiten unit-testbar (Wind, Sicht, Wolken, Wettererscheinungen,
Flugkategorie, TAF-Änderungsgruppen).

## APK direkt am Handy bauen (GitHub Actions)

Der Workflow [`build-apk.yml`](.github/workflows/build-apk.yml) baut die App auf
GitHub-Servern (mit vollem Internetzugriff, ohne die Einschränkungen dieser Sandbox)
und stellt eine fertige Debug-APK zum Download bereit – ganz ohne eigenen Rechner:

1. In der **GitHub-App** (oder im mobilen Browser) zum Repository → Tab **Actions**
   → Workflow **"APK bauen"** gehen.
2. Oben rechts **"Run workflow"** antippen (funktioniert auch ohne Push, jederzeit
   manuell auslösbar) – oder einfach abwarten, der Workflow läuft automatisch bei
   jedem Push auf einen beliebigen Branch mit.
3. Nach ca. 3–5 Minuten ist der Lauf grün. Zwei Wege, an die APK zu kommen:
   - **Release "Letzter Debug-Build"** (Tab **Releases**, oder Link im Workflow-Log):
     Dort liegt `schmuddelwetter-debug-latest.apk` als direkter Download-Link –
     im Browser antippen, herunterladen, in der Dateien-App öffnen, installieren.
     Das ist der bequemste Weg am Handy (kein Einloggen/Entpacken nötig).
   - **Workflow-Artefakt** (im jeweiligen Workflow-Lauf ganz unten): enthält dieselbe
     APK mit Commit-Hash im Namen, liegt als ZIP vor (30 Tage aufbewahrt).
4. Bei der Installation fragt Android nach der Erlaubnis "Apps aus unbekannten
   Quellen installieren" für die verwendete Browser-/Dateien-App – einmalig
   zulassen, dann startet die Installation.

Der Workflow baut bewusst nur eine **Debug-APK** (automatisch debug-signiert von
Android selbst) – für reine Privatnutzung reicht das; ein Release-Signing-Setup mit
eigenem Schlüssel ist für diesen Anwendungsfall nicht nötig.

> Falls das Erstellen des Releases mit einem Berechtigungsfehler abbricht: unter
> **Settings → Actions → General → Workflow permissions** im Repository
> "Read and write permissions" aktivieren.

## Datenquellen & Lizenzen

- Wetterdaten: [Deutscher Wetterdienst, Open Data (MOSMIX)](https://opendata.dwd.de) –
  Nutzung gemäß DWD-Nutzungsbedingungen (freie Nutzung mit Quellenangabe).
- Flugwetter: [aviationweather.gov](https://aviationweather.gov) (NOAA Aviation
  Weather Center), öffentliche US-Regierungsdaten.
- Schriften: "Silkscreen" und "Space Mono" (SIL Open Font License), bezogen aus dem
  offiziellen [google/fonts](https://github.com/google/fonts)-Repository.
- Das App-Design ist von der Nothing-OS-Ästhetik inspiriert, verwendet aber keine
  Nothing-Markenzeichen, -Icons oder die proprietäre "Ndot"-Schrift.

## Bekannte Einschränkungen

- Die Flughafenliste (`assets/airports.json`) ist eine kuratierte Auswahl für Komfort-
  Suche/Umkreissuche. Jeder gültige ICAO-Code funktioniert unabhängig davon direkt.
- Die DWD-ww-Code-Zuordnung (`WeatherCodeMapper`) ist eine sinnvolle Vereinfachung der
  WMO-Codetabelle 4677/4680, keine 1:1-Übersetzung jedes Einzelcodes.
- Kein Offline-Modus: Ohne Internetverbindung können weder DWD- noch METAR/TAF-Daten
  geladen werden (nur die zuletzt bekannte Position wird lokal zwischengespeichert).
- Der Wetter-Refresh lädt jedes Mal die komplette DWD-MOSMIX-Sammeldatei (~36 MB,
  siehe oben) – im mobilen Datennetz spürbar. Ein WLAN-Hinweis oder ein selteneres
  automatisches Aktualisieren wären sinnvolle Erweiterungen, sind aber (noch) nicht
  eingebaut.
