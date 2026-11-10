import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Optional: Name und Erreichbarkeit der verantwortlichen Stelle, z.B.
 * VITE_CONTACT_INFO="Turnverein Muster, Vorstand, vorstand@example.ch".
 */
const CONTACT_INFO = (import.meta.env.VITE_CONTACT_INFO as string | undefined)?.trim();

export default function PrivacyPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="text-page">
      <h1>Datenschutz</h1>
      <p>
        Das Materialarchiv ist ein internes Werkzeug des Vereins. Diese Seite erklärt, welche Angaben über dich
        gespeichert werden und wer sie sehen kann.
      </p>

      <h2 className="section-title">Verantwortlich</h2>
      <p>
        {CONTACT_INFO
          ? CONTACT_INFO
          : "Verantwortlich ist der Vorstand des Vereins. Bei Fragen wende dich an ein Vorstandsmitglied oder an einen Admin des Archivs."}
      </p>

      <h2 className="section-title">Welche Daten gespeichert werden</h2>
      <ul>
        <li>
          <strong>Dein Konto:</strong> Name, E-Mail-Adresse, Telefonnummer, deine Rolle und das Datum der
          Registrierung. Dein Passwort wird nur verschlüsselt (als nicht umkehrbarer Hash) gespeichert – niemand
          kann es auslesen, auch kein Admin.
        </li>
        <li>
          <strong>Was du im Archiv tust:</strong> beantragte Bestandsänderungen mit Datum und Notiz, von dir
          angelegte Events, reserviertes Material und Anfragen, bei einem Event mitzuhelfen.
        </li>
        <li>
          <strong>Technisch:</strong> Zum Schutz vor dem Durchprobieren von Passwörtern merkt sich der Server
          Fehlversuche bei der Anmeldung für kurze Zeit zusammen mit der IP-Adresse.
        </li>
      </ul>

      <h2 className="section-title">Wer was sehen kann</h2>
      <ul>
        <li>Alle angemeldeten Mitglieder sehen das Material, die Events und die Namen von Erstellern und Mithelfern.</li>
        <li>Deine E-Mail-Adresse und Telefonnummer sehen nur die Admins des Archivs.</li>
        <li>Ohne freigegebenes Konto ist nichts davon einsehbar.</li>
      </ul>

      <h2 className="section-title">Speicherung auf deinem Gerät</h2>
      <p>
        Nach der Anmeldung legt die Seite ein Anmelde-Token im Speicher deines Browsers ab, damit du angemeldet
        bleibst. Es werden keine Cookies gesetzt, keine Auswertungen über dein Verhalten erstellt und keine Dienste
        von Dritten eingebunden.
      </p>

      <h2 className="section-title">Wie lange die Daten bleiben</h2>
      <p>
        Deine Kontodaten bleiben gespeichert, bis dein Konto gelöscht wird. Events und Bestandsänderungen bleiben
        danach für den Verein erhalten, aber ohne deinen Namen. In Datensicherungen können deine Angaben noch
        einige Wochen enthalten sein, bis die Sicherung überschrieben wird.
      </p>

      <h2 className="section-title">Deine Möglichkeiten</h2>
      <ul>
        <li>Name, E-Mail-Adresse, Telefonnummer und Passwort änderst du selbst im Profil.</li>
        <li>
          Möchtest du wissen, was über dich gespeichert ist, oder dein Konto löschen lassen, wende dich an einen
          Admin des Archivs oder an den Vorstand.
        </li>
      </ul>

      <p>
        <Link to={isAuthenticated ? "/profile" : "/login"}>
          {isAuthenticated ? "Zurück zum Profil" : "Zurück zur Anmeldung"}
        </Link>
      </p>
    </div>
  );
}
