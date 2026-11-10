import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { register } from "../api/auth";
import { ApiError } from "../api/client";

export default function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [registered, setRegistered] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({ name, email, phoneNumber, password });
      setRegistered(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Registrierung fehlgeschlagen");
    } finally {
      setLoading(false);
    }
  }

  if (registered) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <h1 className="auth-card__title">Konto erstellt</h1>
          <p className="auth-card__subtitle">
            Ein Admin des Vereins muss dein Konto noch freigeben. Sobald das erledigt ist, kannst du dich mit
            deiner E-Mail und deinem Passwort anmelden.
          </p>
          <Link to="/login" className="btn btn--primary btn--block">
            Zur Anmeldung
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1 className="auth-card__title">Konto erstellen</h1>
        <p className="auth-card__subtitle">Für Mitglieder des Vereins</p>

        <label className="field">
          <span>Name</span>
          <input required value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
        </label>

        <label className="field">
          <span>E-Mail</span>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            inputMode="email"
          />
        </label>

        <label className="field">
          <span>Telefonnummer</span>
          <input
            required
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            autoComplete="tel"
            inputMode="tel"
          />
        </label>

        <label className="field">
          <span>Passwort</span>
          <input
            type="password"
            required
            minLength={8}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="new-password"
          />
          <small className="hint">Mindestens 8 Zeichen, davon ein Buchstabe und eine Zahl.</small>
        </label>

        {error && <p className="form-error" role="alert">{error}</p>}

        <button type="submit" className="btn btn--primary btn--block" disabled={loading}>
          {loading ? "Wird erstellt…" : "Registrieren"}
        </button>

        <p className="auth-card__footer">
          Schon ein Konto? <Link to="/login">Anmelden</Link>
        </p>
      </form>
    </div>
  );
}
