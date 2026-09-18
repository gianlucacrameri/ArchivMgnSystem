# Materialarchiv – Frontend

Responsive Web-App (React + TypeScript + Vite) für das Materialarchiv des
Sportvereins. Mobile-first gebaut und primär auf iPhone/Safari getestet,
funktioniert aber genauso auf dem Laptop.

## Voraussetzungen

- Node.js 20+
- Das Backend (`../`) muss erreichbar sein (lokal oder gehostet)

## Setup

```bash
cd frontend
npm install
cp .env.example .env   # VITE_API_BASE_URL ggf. anpassen
npm run dev
```

Die App läuft dann unter `http://localhost:5173` und spricht standardmässig
das Backend unter `http://localhost:8080` an.

## Build

```bash
npm run build      # tsc + vite build -> dist/
npm run preview    # gebauten Stand lokal ansehen
```

`dist/` kann als statisches Bundle auf einem beliebigen Webserver oder z.B.
in `spring-boot`s `src/main/resources/static` ausgeliefert werden.

## Auf dem iPhone testen

1. `npm run dev -- --host` (macht den Dev-Server im WLAN erreichbar)
2. Auf dem iPhone im selben WLAN `http://<Laptop-IP>:5173` in Safari öffnen
3. Optional: über "Teilen → Zum Home-Bildschirm" hinzufügen – die App startet
   dann ohne Safari-Adressleiste (Meta-Tags dafür sind in `index.html`
   bereits gesetzt).

## Struktur

- `src/api/` – typisierte Fetch-Wrapper pro Backend-Ressource
- `src/context/` – Auth- (JWT/Rolle) und Toast-Kontext
- `src/components/` – wiederverwendbare UI-Bausteine (Modal, Karten, Nav, …)
- `src/pages/` – Seiten/Routen (Archiv, Events, Anfragen, Login/Register)
- `src/styles/index.css` – ein zentrales, mobile-first Stylesheet

## Bekannte Einschränkungen / nächste Schritte

- Item-Fotos hängen hinter dem Login (JWT) und werden deshalb per `fetch` +
  Blob-URL geladen (`AuthenticatedImage`), nicht per normalem `<img src>`.
- Es gibt noch keinen Offline-/PWA-Modus (Service Worker); die Meta-Tags für
  "Zum Home-Bildschirm hinzufügen" sind aber vorbereitet.
- Für produktiven Einsatz: `VITE_API_BASE_URL` auf die echte Backend-URL
  setzen und das Backend über HTTPS betreiben (JWT sollte nie über
  unverschlüsseltes HTTP übertragen werden).
