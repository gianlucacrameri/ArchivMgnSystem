import { useCallback, useEffect, useState } from "react";
import type { UserDTO } from "../types";
import { approveUser, deleteUser, fetchUsers, resetUserPassword, updateUserRole } from "../api/users";
import { notifyAdminCountsChanged } from "../api/admin";
import Modal from "../components/Modal";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { useAuth } from "../context/AuthContext";

function formatDate(value?: string): string {
  if (!value) return "";
  return new Date(value).toLocaleDateString("de-CH", { day: "2-digit", month: "2-digit", year: "numeric" });
}

export default function MembersPage() {
  const { showError, showSuccess } = useToast();
  const { userId } = useAuth();
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [temporaryPassword, setTemporaryPassword] = useState<{ username: string; password: string } | null>(null);

  const load = useCallback(async () => {
    try {
      setUsers(await fetchUsers());
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Mitglieder konnten nicht geladen werden");
    } finally {
      setLoading(false);
    }
  }, [showError]);

  useEffect(() => {
    load();
  }, [load]);

  async function run(id: number, action: () => Promise<void>, successMessage: string) {
    setProcessingId(id);
    try {
      await action();
      showSuccess(successMessage);
      notifyAdminCountsChanged();
      await load();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Aktion fehlgeschlagen");
    } finally {
      setProcessingId(null);
    }
  }

  function handleApprove(user: UserDTO) {
    run(user.id, () => approveUser(user.id), `${user.username} wurde freigegeben`);
  }

  function handleReject(user: UserDTO) {
    if (!window.confirm(`Anmeldung von "${user.username}" ablehnen und das Konto löschen?`)) return;
    run(user.id, () => deleteUser(user.id), "Anmeldung abgelehnt");
  }

  function handleToggleAdmin(user: UserDTO) {
    const makeAdmin = user.role !== "ADMIN";
    const question = makeAdmin
      ? `"${user.username}" zum Admin machen? Admins können Material, Anträge und Mitglieder verwalten.`
      : `"${user.username}" die Admin-Rechte entziehen?`;
    if (!window.confirm(question)) return;
    run(
      user.id,
      () => updateUserRole(user.id, makeAdmin ? "ADMIN" : "USER"),
      makeAdmin ? `${user.username} ist jetzt Admin` : `${user.username} ist jetzt Mitglied`
    );
  }

  async function handleResetPassword(user: UserDTO) {
    const question =
      `Für "${user.username}" ein neues, vorläufiges Passwort erstellen? ` +
      "Das bisherige Passwort gilt danach nicht mehr.";
    if (!window.confirm(question)) return;
    setProcessingId(user.id);
    try {
      const password = await resetUserPassword(user.id);
      setTemporaryPassword({ username: user.username, password });
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Zurücksetzen fehlgeschlagen");
    } finally {
      setProcessingId(null);
    }
  }

  async function handleCopyPassword() {
    if (!temporaryPassword) return;
    try {
      await navigator.clipboard.writeText(temporaryPassword.password);
      showSuccess("Passwort kopiert");
    } catch {
      showError("Kopieren nicht möglich – bitte von Hand abschreiben");
    }
  }

  function handleRemove(user: UserDTO) {
    const question =
      `"${user.username}" wirklich entfernen? Das Konto wird gelöscht. ` +
      "Events und die Bestandshistorie dieses Mitglieds bleiben erhalten.";
    if (!window.confirm(question)) return;
    run(user.id, () => deleteUser(user.id), `${user.username} wurde entfernt`);
  }

  // Admins gelten immer als freigegeben (der erste Admin wird per SQL ernannt).
  const pending = users.filter((u) => u.approved === false && u.role !== "ADMIN");
  const members = users
    .filter((u) => !pending.includes(u))
    .sort((a, b) => a.username.localeCompare(b.username, "de"));

  return (
    <div className="page">
      <div className="page__header">
        <h1>Mitglieder</h1>
      </div>

      {loading && <p className="hint">Lädt…</p>}

      {pending.length > 0 && (
        <>
          <h2 className="section-title">Warten auf Freigabe</h2>
          <p className="hint">Diese Personen haben sich registriert und können sich erst nach deiner Freigabe anmelden.</p>
          <ul className="request-list">
            {pending.map((u) => (
              <li key={u.id} className="request-card">
                <div className="request-card__info">
                  <strong>{u.username}</strong>
                  <span className="event-card__meta">{u.email}</span>
                  <span className="event-card__meta">
                    {u.phoneNumber}
                    {u.createdAt ? ` · registriert am ${formatDate(u.createdAt)}` : ""}
                  </span>
                </div>
                <div className="button-row">
                  <button
                    type="button"
                    className="btn btn--primary"
                    disabled={processingId === u.id}
                    onClick={() => handleApprove(u)}
                  >
                    Freigeben
                  </button>
                  <button
                    type="button"
                    className="btn btn--secondary"
                    disabled={processingId === u.id}
                    onClick={() => handleReject(u)}
                  >
                    Ablehnen
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <hr className="divider" />
        </>
      )}

      {!loading && (
        <>
          <h2 className="section-title">Freigegebene Konten ({members.length})</h2>
          <ul className="request-list">
            {members.map((u) => {
              const isSelf = u.id === userId;
              return (
                <li key={u.id} className="request-card">
                  <div className="request-card__info">
                    <strong>
                      {u.username}
                      {isSelf ? " (du)" : ""}
                    </strong>
                    <span className="event-card__meta">{u.email}</span>
                    <span className="event-card__meta">{u.phoneNumber}</span>
                    {u.role === "ADMIN" && <span className="badge badge--neutral">Admin</span>}
                  </div>
                  {!isSelf && (
                    <div className="button-row">
                      <button
                        type="button"
                        className="btn btn--link"
                        disabled={processingId === u.id}
                        onClick={() => handleToggleAdmin(u)}
                      >
                        {u.role === "ADMIN" ? "Admin-Rechte entziehen" : "Zum Admin machen"}
                      </button>
                      <button
                        type="button"
                        className="btn btn--link"
                        disabled={processingId === u.id}
                        onClick={() => handleResetPassword(u)}
                      >
                        Passwort zurücksetzen
                      </button>
                      <button
                        type="button"
                        className="btn btn--link btn--danger-text"
                        disabled={processingId === u.id}
                        onClick={() => handleRemove(u)}
                      >
                        Entfernen
                      </button>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        </>
      )}

      {temporaryPassword && (
        <Modal title="Vorläufiges Passwort" onClose={() => setTemporaryPassword(null)}>
          <div className="stacked-form">
            <p className="hint">
              Gib dieses Passwort persönlich an {temporaryPassword.username} weiter. Es wird nur jetzt angezeigt.
              Nach der Anmeldung wird {temporaryPassword.username} aufgefordert, ein eigenes Passwort zu setzen.
            </p>
            <p className="temp-password">{temporaryPassword.password}</p>
            <div className="button-row">
              <button type="button" className="btn btn--primary" onClick={handleCopyPassword}>
                Kopieren
              </button>
              <button type="button" className="btn btn--secondary" onClick={() => setTemporaryPassword(null)}>
                Schliessen
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
