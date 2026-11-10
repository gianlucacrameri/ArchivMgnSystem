import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { fetchCurrentUser, logoutEverywhere, updateOwnProfile, type CurrentUser } from "../api/users";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";
import { useAuth } from "../context/AuthContext";
import BackupPanel from "../components/BackupPanel";

const roleLabels: Record<string, string> = {
  ADMIN: "Admin",
  MANAGER: "Manager",
  USER: "Mitglied",
};

export default function ProfilePage() {
  const { showSuccess, showError } = useToast();
  const { replaceToken, logout, isAdmin, passwordResetRequired } = useAuth();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  const [editMode, setEditMode] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    fetchCurrentUser()
      .then((u) => {
        setUser(u);
        setUsername(u.username);
        setEmail(u.email);
        setPhoneNumber(u.phoneNumber);
      })
      .catch((err) => showError(err instanceof ApiError ? err.message : "Profil konnte nicht geladen werden"))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    if (!user) return;
    setSaving(true);
    try {
      const result = await updateOwnProfile(user.id, {
        username,
        email,
        phoneNumber,
        password: password || undefined,
        currentPassword: needsCurrentPassword ? currentPassword : undefined,
      });
      // Nach einer Änderung von E-Mail oder Passwort gilt nur noch das neue Token.
      if (result.token) replaceToken(result.token);
      showSuccess("Profil aktualisiert");
      setUser({ ...user, username, email, phoneNumber });
      setPassword("");
      setCurrentPassword("");
      setEditMode(false);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Speichern fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  }

  async function handleLogoutEverywhere() {
    if (!window.confirm("Auf allen Geräten abmelden? Du musst dich danach überall neu anmelden.")) return;
    try {
      await logoutEverywhere();
      logout();
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Abmelden fehlgeschlagen");
    }
  }

  // E-Mail und Passwort sind der Schlüssel zum Konto - wer sie ändert, muss
  // zeigen, dass er das aktuelle Passwort kennt.
  const needsCurrentPassword = !!user && (password !== "" || email.trim().toLowerCase() !== user.email.toLowerCase());

  if (loading) return <div className="page"><p className="hint">Lädt…</p></div>;
  if (!user) return <div className="page"><p className="hint">Profil konnte nicht geladen werden.</p></div>;

  return (
    <div className="page">
      <div className="page__header">
        <h1>Mein Profil</h1>
      </div>

      {passwordResetRequired && !editMode && (
        <div className="notice-banner" role="status">
          <span>Du bist mit einem vorläufigen Passwort angemeldet. Bitte setze ein eigenes.</span>
          <button type="button" className="btn btn--primary" onClick={() => setEditMode(true)}>
            Passwort ändern
          </button>
        </div>
      )}

      {!editMode && (
        <>
          <dl className="item-detail__facts item-detail__facts--2col">
            <div>
              <dt>Name</dt>
              <dd>{user.username}</dd>
            </div>
            <div>
              <dt>Rolle</dt>
              <dd>{roleLabels[user.role] ?? user.role}</dd>
            </div>
            <div>
              <dt>E-Mail</dt>
              <dd>{user.email}</dd>
            </div>
            <div>
              <dt>Telefonnummer</dt>
              <dd>{user.phoneNumber}</dd>
            </div>
          </dl>

          <div className="button-row">
            <button type="button" className="btn btn--secondary" onClick={() => setEditMode(true)}>
              Bearbeiten
            </button>
            <button type="button" className="btn btn--link" onClick={handleLogoutEverywhere}>
              Auf allen Geräten abmelden
            </button>
          </div>
          <p className="hint">
            Eine Anmeldung bleibt 30 Tage gültig. Hast du ein Gerät verloren, meldest du dich hier überall ab.
          </p>
          <p className="hint">
            <Link to="/datenschutz" className="text-link">
              Datenschutz
            </Link>
          </p>

          {isAdmin && <BackupPanel />}
        </>
      )}

      {editMode && (
        <form onSubmit={handleSave} className="stacked-form">
          <label className="field">
            <span>Name</span>
            <input required value={username} onChange={(e) => setUsername(e.target.value)} />
          </label>
          <label className="field">
            <span>E-Mail</span>
            <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <label className="field">
            <span>Telefonnummer</span>
            <input required value={phoneNumber} onChange={(e) => setPhoneNumber(e.target.value)} />
          </label>
          <label className="field">
            <span>Neues Passwort (optional)</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              minLength={8}
              autoComplete="new-password"
            />
            <small className="hint">
              Nur ausfüllen, um es zu ändern. Mindestens 8 Zeichen, davon ein Buchstabe und eine Zahl. Andere
              Geräte werden dabei abgemeldet.
            </small>
          </label>

          {needsCurrentPassword && (
            <label className="field">
              <span>Aktuelles Passwort</span>
              <input
                type="password"
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                autoComplete="current-password"
              />
              <small className="hint">Nötig, um E-Mail oder Passwort zu ändern.</small>
            </label>
          )}

          <div className="button-row">
            <button type="submit" className="btn btn--primary" disabled={saving}>
              {saving ? "Speichert…" : "Speichern"}
            </button>
            <button
              type="button"
              className="btn btn--secondary"
              onClick={() => {
                setEditMode(false);
                setUsername(user.username);
                setEmail(user.email);
                setPhoneNumber(user.phoneNumber);
                setPassword("");
                setCurrentPassword("");
              }}
            >
              Abbrechen
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
