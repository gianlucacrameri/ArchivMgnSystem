# ArchivMgnSystem

Spring-Boot-Backend für das Materialarchiv eines Sportvereins.

## Konfiguration

Zugangsdaten und Secrets werden **nicht** im Repository abgelegt, sondern über
Umgebungsvariablen bereitgestellt (siehe `src/main/resources/application.properties`):

| Variable       | Pflicht | Beschreibung                                  | Default (lokal)                              |
|----------------|---------|------------------------------------------------|-----------------------------------------------|
| `DB_URL`       | nein    | JDBC-URL der MySQL-Datenbank                   | `jdbc:mysql://localhost:3306/inventory_dp`    |
| `DB_USERNAME`  | nein    | Datenbank-Benutzer                             | `root`                                        |
| `DB_PASSWORD`  | ja      | Datenbank-Passwort                             | –                                              |
| `JWT_SECRET`   | ja      | Secret zum Signieren der JWTs (mind. 32 Zeichen)| –                                              |

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
`USER`. Höhere Rollen können ausschliesslich von einem bestehenden Admin über
`PUT /api/users/update/{id}` vergeben werden.

## Bestandsänderungen (Freigabe-Workflow)

1. Ein Mitglied beantragt eine Zubuchung (`/api/transactions/add`) oder
   Entnahme (`/api/transactions/use`). Die Anfrage wird als `Transaction`
   mit Status `PENDING` gespeichert – der Lagerbestand ändert sich noch nicht.
2. Ein Admin sieht offene Anfragen über `GET /api/transactions/all` (Filter
   z.B. nach `PENDING`) und bestätigt oder lehnt sie ab:
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
