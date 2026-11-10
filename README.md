# ArchivMgnSystem

Spring-Boot-Backend für das Materialarchiv eines Sportvereins.
Das zugehörige Frontend (React, mobile-first) liegt in [`frontend/`](frontend/README.md).

## Konfiguration

Zugangsdaten und Secrets werden **nicht** im Repository abgelegt, sondern über
Umgebungsvariablen bereitgestellt (siehe `src/main/resources/application.properties`):

| Variable       | Pflicht | Beschreibung                                  | Default (lokal)                              |
|----------------|---------|------------------------------------------------|-----------------------------------------------|
| `DB_URL`       | nein    | JDBC-URL der MySQL-Datenbank                   | `jdbc:mysql://localhost:3306/inventory_dp`    |
| `DB_USERNAME`  | nein    | Datenbank-Benutzer                             | `root`                                        |
| `DB_PASSWORD`  | ja      | Datenbank-Passwort                             | –                                              |
| `JWT_SECRET`   | ja      | Secret zum Signieren der JWTs (mind. 32 Zeichen)| –                                              |
| `JWT_EXPIRATION_DAYS` | nein | Gültigkeit einer Anmeldung in Tagen        | `30`                                          |
| `APP_CORS_ALLOWED_ORIGINS` | nur bei getrennten Domains | Adresse(n) des Frontends, mit Komma getrennt | leer = eigene Installation (siehe unten) |
| `APP_UPLOAD_DIR` | nein  | Ordner für hochgeladene Fotos                  | `<Projektverzeichnis>/uploads/items`          |
| `APP_BACKUP_DIR` | produktiv ja | Ordner für die nächtliche Datensicherung (siehe unten) | leer = keine Sicherung                 |
| `APP_BACKUP_KEEP_DAYS` | nein | So lange bleiben Datenbank-Sicherungen erhalten | `30`                                     |
| `APP_BACKUP_CRON` | nein | Zeitpunkt der Sicherung (Spring-Cron-Format)   | `0 0 3 * * *` (täglich 03:00)                 |
| `APP_BACKUP_MYSQLDUMP_PATH` | nein | Pfad zu `mysqldump`, falls nicht im Suchpfad | `mysqldump`                              |

Beispiel (lokal, bash):

```bash
export DB_PASSWORD="dein-db-passwort"
export JWT_SECRET="ein-zufaelliges-langes-secret"
./mvnw spring-boot:run
```

In produktiven Umgebungen (Docker, CI/CD, Hosting) die Variablen über die
jeweilige Secret-Verwaltung setzen (z.B. Docker `--env-file`, GitHub Actions
Secrets, etc.) – niemals echte Werte committen.

## Rollen

- `USER` (Mitglied): kann das Archiv einsehen, Bestandsänderungen beantragen
  (`POST /api/transactions/add` bzw. `/use`), Events anlegen und Material
  dafür reservieren.
- `ADMIN`: zusätzlich Kategorien verwalten, Nutzer verwalten/Rollen vergeben,
  und offene Bestandsänderungen freigeben oder ablehnen
  (`PUT /api/transactions/{id}`).

Selbstregistrierung (`POST /api/auth/register`) vergibt immer die Rolle
`USER`, und das neue Konto ist zunächst gesperrt: Ein Admin muss es unter
**Mitglieder** freigeben, bevor eine Anmeldung möglich ist. Dort vergibt oder
entzieht ein Admin auch Admin-Rechte und entfernt Mitglieder (deren Events und
Bestandshistorie bleiben erhalten).

Den allerersten Admin ernennt man einmalig direkt in der Datenbank – Admins
brauchen keine Freigabe:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'deine@mail.ch';
```

## Anmeldung & Sicherheit

- Passwörter: mindestens 8 Zeichen, davon ein Buchstabe und eine Zahl.
- Nach 5 Fehlversuchen ist die Anmeldung für diese E-Mail 15 Minuten gesperrt.
  Die Fehlermeldung verrät nicht, ob die E-Mail existiert.
- Wer E-Mail oder Passwort im Profil ändert, muss das aktuelle Passwort
  eingeben.
- **Passwort vergessen:** Ein Admin erstellt unter **Mitglieder** ein
  vorläufiges Passwort („Passwort zurücksetzen"). Es wird einmalig angezeigt
  und persönlich weitergegeben; nach der Anmeldung fordert die Seite dazu
  auf, ein eigenes Passwort zu setzen.
- Eine Anmeldung gilt 30 Tage (`JWT_EXPIRATION_DAYS`). Ein Passwortwechsel
  oder „Auf allen Geräten abmelden" im Profil macht alle bisherigen
  Anmeldungen des Kontos sofort ungültig; gelöschte Konten verlieren den
  Zugriff ebenfalls sofort.

## Datensicherung

Sobald `APP_BACKUP_DIR` gesetzt ist, sichert das Backend jede Nacht um 03:00:

```
<APP_BACKUP_DIR>/datenbank/archiv-2026-01-31_030000.sql.gz   vollständiger Abzug der Datenbank
<APP_BACKUP_DIR>/fotos/…                                      Kopie aller hochgeladenen Fotos
```

- Voraussetzung: Das Programm `mysqldump` (Teil des MySQL-Clients) ist auf dem
  Server installiert.
- Datenbank-Sicherungen werden nach `APP_BACKUP_KEEP_DAYS` Tagen gelöscht
  (die drei neuesten bleiben immer). Fotos werden nur ergänzt, nie gelöscht –
  so passt auch eine ältere Datenbank-Sicherung noch zu den Fotos.
- Admins sehen im **Profil** unter „Datensicherung", wann zuletzt gesichert
  wurde, ob ein Fehler auftrat, und können eine Sicherung von Hand starten.
- **Wichtig:** Der Backup-Ordner sollte auf einem anderen Datenträger liegen
  als Datenbank und Fotos (zweite Festplatte, Netzlaufwerk) oder regelmässig
  an einen anderen Ort kopiert werden. Eine Sicherung auf derselben Festplatte
  hilft bei einem Defekt nicht.
- Auch `APP_UPLOAD_DIR` sollte produktiv auf einen festen Ordner ausserhalb
  des Projektverzeichnisses zeigen.

Wiederherstellen:

```bash
# 1. Datenbank einspielen (in eine leere Datenbank)
gunzip -c <APP_BACKUP_DIR>/datenbank/archiv-JJJJ-MM-TT_HHMMSS.sql.gz | mysql -u root -p inventory_dp
# 2. Fotos zurückkopieren
cp <APP_BACKUP_DIR>/fotos/* <APP_UPLOAD_DIR>/
```

Die Vorschaubilder erzeugt das Backend danach beim ersten Aufruf selbst neu.

## Produktiver Betrieb

- **HTTPS:** Das Backend spricht selbst nur HTTP. Davor gehört ein
  Reverse-Proxy mit Zertifikat (z.B. Caddy oder nginx mit Let's Encrypt), der
  das Frontend ausliefert und `/api` an das Backend weiterreicht. Ohne HTTPS
  gehen Passwörter und Login-Tokens im Klartext über das Netz.
- **Erlaubte Herkunft:** Ohne weitere Einstellung nimmt das Backend Aufrufe
  nur von Seiten an, die zur eigenen Installation gehören: localhost, private
  Netze, jede Adresse dieses Rechners sowie die Adresse, unter der auch das
  Backend angesprochen wird (Frontend und Backend auf derselben Domain).
  Fremde Webseiten werden abgelehnt. Läuft das Frontend auf einer anderen
  Domain als das Backend, muss sie in `APP_CORS_ALLOWED_ORIGINS` stehen, z.B.
  `https://archiv.example.ch` – dann gilt ausschliesslich diese Liste. Lehnt
  das Backend eine Seite ab, steht das als Warnung im Backend-Log.
- **Hinter einem Reverse-Proxy** zusätzlich
  `SERVER_FORWARD_HEADERS_STRATEGY=native` setzen, damit das Backend die
  echte Adresse der Besucher sieht (für den Schutz vor zu vielen
  Fehlversuchen) statt nur die des Proxys.
- Frontend mit `VITE_API_BASE_URL=https://…` bauen (siehe
  [`frontend/README.md`](frontend/README.md)).
- **Schutz im Browser:** Die gebaute Seite enthält eine
  Content-Security-Policy. Der Browser führt damit nur Skripte der Seite
  selbst aus und erlaubt Datenverkehr nur zum Backend aus
  `VITE_API_BASE_URL` – die Adresse muss deshalb beim Bauen stimmen.
- **Datenschutz-Seite:** `/datenschutz` beschreibt, welche Daten gespeichert
  werden. Die verantwortliche Stelle lässt sich beim Bauen über
  `VITE_CONTACT_INFO` eintragen. Der Text sollte vor dem Livegang vom
  Vorstand geprüft werden.

## Bestandsänderungen (Freigabe-Workflow)

1. Ein Mitglied beantragt eine Zubuchung (`/api/transactions/add`) oder
   Entnahme (`/api/transactions/use`). Die Anfrage wird als `Transaction`
   mit Status `PENDING` gespeichert – der Lagerbestand ändert sich noch nicht.
2. Das Mitglied sieht den Stand seiner eigenen Anträge unter **Anträge**
   (`GET /api/transactions/mine`). Ein Admin sieht alle offenen Anfragen über
   `GET /api/transactions/all` (Filter z.B. nach `PENDING`) und bestätigt
   oder lehnt sie ab:
   `PUT /api/transactions/{id}` mit Body `"COMPLETED"` oder `"CANCELED"`.
3. Erst bei `COMPLETED` wird der Lagerbestand angepasst. Eine bereits
   entschiedene Anfrage kann nicht erneut verändert werden.

## Events & Reservationen

- `POST /api/events` – eigenes Event anlegen (Name, Zeitraum, Beschreibung).
- `GET /api/events` / `GET /api/events/{id}` – alle Events bzw. ein Event
  inkl. aller reservierten Materialien einsehen.
- `POST /api/events/{id}/reservations` – Material für den Event-Zeitraum
  reservieren; die Verfügbarkeit wird gegen den aktuellen Lagerbestand und
  bereits bestehende, sich zeitlich überschneidende Reservierungen anderer
  Events geprüft.
- `DELETE /api/events/{id}/reservations/{reservationId}` – Reservierung
  entfernen. Ändern/Löschen eines Events ist nur dem Ersteller oder einem
  Admin erlaubt.

## Material suchen & Fotos

- `GET /api/items/all?filter=&categoryId=&page=&size=` – Volltextsuche über
  Name, SKU, Lagerort, Beschreibung und Kategorie, mit Paging.
- Fotos werden beim Anlegen/Bearbeiten eines Items als `multipart/form-data`
  hochgeladen (`imageFile`), serverseitig unter `app.upload-dir` (Default:
  `<Projektverzeichnis>/uploads/items`) gespeichert und über
  `GET /api/items/images/{filename}` ausgeliefert. `Item.imageUrl` enthält
  direkt diesen abrufbaren Pfad.
- Das Frontend verkleinert Fotos vor dem Upload auf höchstens 2000 Pixel
  Kantenlänge; der Server erzeugt daraus Vorschaubilder
  (`?size=thumb` / `?size=medium`). Wird ein Item gelöscht oder ein Foto
  ersetzt, entfernt der Server die zugehörigen Dateien.
- Artikelnummern (`ART-00000`, `ART-00001`, …) vergibt der Server fortlaufend.
  Eine einmal vergebene Nummer wird auch nach dem Löschen nicht wiederverwendet.
