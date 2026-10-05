# DAPS — Dein Diabetes, deine Kontrolle, deine Freiheit! 🚀

![Screenshots-Light](docs/images/collage-light.png)
![Screenshots-Dark](docs/images/collage-dark.png)

> [!WARNING]
> **Experimentelle Software — kein Medizinprodukt.**
>
> DAPS befindet sich in aktiver Entwicklung und ist eine Open-Source-Software. Die Nutzung erfolgt auf eigene Gefahr. Verlasse dich nicht ausschließlich auf diese App für medizinische Entscheidungen oder Insulindosierungen. Überprüfe wichtige Messwerte stets mit den offiziellen Geräten deiner Hersteller.

---

## 🌟 Deine Therapie, exakt so flexibel wie dein Leben

**DAPS** (Automated Insulin Delivery / APS) wurde entwickelt, um dir die **maximale Freiheit und volle Kontrolle** über dein Diabetes-Management zurückzugeben. Kein starres Korsett, keine komplizierten Umwege – sondern ein modernes, intelligentes System, das sich geschmeidig an *deinen* Alltag anpasst.

Egal ob im Alltag, beim Sport, beim spontanen Snack zwischendurch oder in der Nacht: DAPS unterstützt dich genau dort, wo du es brauchst, und bleibt dabei erstaunlich einfach und intuitiv.

---

## ✨ Das macht DAPS so besonders

### 📊 Übersichtlich & Alles auf einen Blick
Ein **modernes, aufgeräumtes Dashboard** zeigt dir sekundenschnell genau das, was jetzt zählt: deine aktuellen Glukosewerte, Trends, aktives Insulin (IOB), aktive Kohlenhydrate (COB) und den Status deines Loops. Kein Suchen, kein Überladen – einfach klar und verständlich.

### ⚙️ Maximale Freiheit bei den Einstellungen
Dein Diabetes ist so individuell wie du. DAPS bietet dir **unzählige Einstellmöglichkeiten**, um Zielbereiche, Basalraten, Faktoren und Algorithmen exakt auf deine persönlichen Bedürfnisse abzustimmen. Du kannst flexibel **einzelne Werte spontan übersteuern** oder komplette **Presets** für bestimmte Lebenssituationen wie Sport, Krankheit oder Stress konfigurieren. Du behältst jederzeit das Steuer in der Hand.

### 💡 Einfach & Selbsterklärend
Kein langes Einarbeiten nötig! Die Benutzeroberfläche ist von Grund auf so gestaltet, dass du dich **sofort zurechtfindest**. Klare Icons, verständliche Dialoge und ein durchdachtes Bedienkonzept machen die tägliche Nutzung spielend leicht.

### 🔍 Volle Transparenz & Maximale Dateneinsicht
Keine Blackbox! DAPS stellt dir **alle wichtigen Daten übersichtlich bereit**: von aktuellen Blutzuckerwerten, aktivem Insulin (IOB) und aktiven Kohlenhydraten (COB) über die exakte Mahlzeiten- und Insulin-Wirkung bis hin zu einem **detaillierten Log**. Jede Entscheidung des Algorithmus bleibt dadurch für dich jederzeit transparent und nachvollziehbar.

### 🍽️ Flexible Mahlzeitendeklaration
Schneller Snack, ausgiebiges Menü oder fett- und eiweißreiche Speisen? Erfasse deine Mahlzeiten genau so, wie es für dich am besten passt. Die **flexible Eingabe** macht die Erfassung von Kohlenhydraten unkompliziert und extrem anpassungsfähig.

### 💉 Intelligente Insulinplanung
Plane Bolusgaben und Mahlzeiten voraus! Mit der integrierten **Insulinplanung** berechnet DAPS präzise Vorschläge, berücksichtigt Wirkprofile und hilft dir, Mahlzeiten-Spitzen effektiv abzufangen. Auch für schwierige Situationen wie Pizza, Ofenkäse, Fleisch etc.

### 🔌 Einfache Anbindung von CGM & Pumpen
DAPS setzt auf ein modulares System: Zur Anbindung neuer CGM-Systeme und Insulinpumpen muss lediglich ein **sehr einfaches Kotlin-Interface** implementiert werden. Die Entwicklung und Einbindung echter Hardware-Treiber ist derzeit aktiv im Gange. Du möchtest mitmachen und einen eigenen CGM- oder Pumpentreiber beisteuern? Melde dich gerne!

### ⚡ Hochperformant & Modern
DAPS ist eine echte **Greenfield-Entwicklung** auf Basis neuester Android-Technologien (Kotlin & Jetpack Compose). Das bedeutet: blitzschnelle Reaktionen, minimaler Akkuverbrauch und flüssige Animationen – im eleganten Light- und Dark-Mode.

---

## 🚀 Für Entwickler & Neugierige

Du möchtest DAPS ausprobieren oder mitgestalten? Das Projekt setzt auf eine **moderne, modulare Architektur** mit strikter Trennung von Core-Engine, CGM-Schnittstellen und Pumpen-Anbindung.

### 🛠️ Quickstart

1. Repository klonen:
   ```bash
   git clone https://github.com/Albert78/DAPS.git
   ```
2. Projekt in **Android Studio** öffnen.
3. Direkt auf deinem Android-Smartphone ausführen (`simDebug`-Flavor für den integrierten Körper-Simulator nutzen!).

*Hinweis: Bei großen Updates kann ein Löschen der lokalen App-Daten erforderlich sein (App-Info ➔ Speicher ➔ Daten löschen).*

### 🧪 Integrierter Körper-Simulator (`sim-body`)
Teste Algorithmen, Mahlzeiten und Reaktionen gefahrlos ohne Hardware! Bau einfach die App im `simDebug`-Build-Flavor und simuliere Glukoseverläufe, Sport oder Stress direkt auf dem Handy.

### 🧱 Tech Stack
* **Sprache:** Kotlin
* **UI & Navigation:** Jetpack Compose & Navigation 3
* **Reaktivität:** Kotlin Coroutines & Flow
* **Persistenz:** Room Database
* **Hintergrund:** Optimierte Android Foreground Services

---

## 📄 Lizenz

Dieses Projekt steht unter der [MIT-Lizenz](LICENSE).