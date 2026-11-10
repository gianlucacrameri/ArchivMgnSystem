import { useEffect, useState } from "react";
import { fetchBackupStatus, runBackup, type BackupStatus } from "../api/admin";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

/** Ab diesem Alter der letzten Sicherung wird gewarnt (geplant ist eine pro Nacht). */
const STALE_AFTER_HOURS = 48;

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("de-CH", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  return `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

/** Zeigt Admins, ob die Datensicherung läuft, und lässt sie von Hand anstossen. */
export default function BackupPanel() {
  const { showError, showSuccess } = useToast();
  const [status, setStatus] = useState<BackupStatus | null>(null);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    fetchBackupStatus()
      .then(setStatus)
      .catch(() => undefined);
  }, []);

  async function handleRun() {
    setRunning(true);
    try {
      const result = await runBackup();
      setStatus(result);
      if (result.lastError) {
        showError("Die Sicherung ist fehlgeschlagen");
      } else {
        showSuccess("Sicherung erstellt");
      }
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Die Sicherung ist fehlgeschlagen");
    } finally {
      setRunning(false);
    }
  }

  if (!status) return null;

  const isStale =
    status.enabled &&
    (!status.lastBackupAt || Date.now() - new Date(status.lastBackupAt).getTime() > STALE_AFTER_HOURS * 3600 * 1000);

  return (
    <section className="backup-panel">
      <hr className="divider" />
      <h2 className="section-title">Datensicherung</h2>

      {!status.enabled && (
        <p className="form-error" role="alert">
          Es ist keine Datensicherung eingerichtet. Geht der Server kaputt, sind Inventar und Fotos verloren. Auf dem
          Server muss dafür APP_BACKUP_DIR auf einen Ordner gesetzt werden (siehe README).
        </p>
      )}

      {status.enabled && (
        <>
          <dl className="item-detail__facts item-detail__facts--2col">
            <div>
              <dt>Letzte Sicherung</dt>
              <dd>
                {status.lastBackupAt
                  ? `${formatDateTime(status.lastBackupAt)} (${formatSize(status.lastBackupSizeBytes ?? 0)})`
                  : "Noch keine"}
              </dd>
            </div>
            <div>
              <dt>Vorhanden</dt>
              <dd>
                {status.databaseBackupCount} Datenbank-Sicherungen, {status.photoCount} Fotos
              </dd>
            </div>
          </dl>
          <p className="hint">
            Gesichert wird jede Nacht nach {status.backupDirectory}. Datenbank-Sicherungen bleiben {status.keepDays}{" "}
            Tage erhalten.
          </p>
          {status.lastError && (
            <p className="form-error" role="alert">
              Die letzte Sicherung ist fehlgeschlagen: {status.lastError}
            </p>
          )}
          {!status.lastError && isStale && (
            <p className="form-error" role="alert">
              Die letzte Sicherung ist älter als zwei Tage. Bitte prüfen, ob der Server nachts läuft.
            </p>
          )}
          <button type="button" className="btn btn--secondary" disabled={running} onClick={handleRun}>
            {running ? "Sichert…" : "Jetzt sichern"}
          </button>
        </>
      )}
    </section>
  );
}
