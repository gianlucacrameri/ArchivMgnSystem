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
