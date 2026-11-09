import { useEffect, useState, type FormEvent } from "react";
import { fetchCurrentUser, updateOwnProfile, type CurrentUser } from "../api/users";
import { ApiError } from "../api/client";
import { useToast } from "../context/ToastContext";

const roleLabels: Record<string, string> = {
  ADMIN: "Admin",
  MANAGER: "Manager",
  USER: "Mitglied",
};

export default function ProfilePage() {
  const { showSuccess, showError } = useToast();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  const [editMode, setEditMode] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
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
      await updateOwnProfile(user.id, {
        username,
        email,
        phoneNumber,
        password: password || undefined,
      });
      showSuccess("Profil aktualisiert");
      setUser({ ...user, username, email, phoneNumber });
      setPassword("");
      setEditMode(false);
    } catch (err) {
      showError(err instanceof ApiError ? err.message : "Speichern fehlgeschlagen");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="page"><p className="hint">Lädt…</p></div>;
  if (!user) return <div className="page"><p className="hint">Profil konnte nicht geladen werden.</p></div>;

  return (
    <div className="page">
      <div className="page__header">
        <h1>Mein Profil</h1>
      </div>

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

          <button type="button" className="btn btn--secondary" onClick={() => setEditMode(true)}>
            Bearbeiten
          </button>
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
              placeholder="Nur ausfüllen, um es zu ändern"
              autoComplete="new-password"
            />
          </label>

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
