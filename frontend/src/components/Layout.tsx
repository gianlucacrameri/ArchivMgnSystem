import { useCallback, useEffect, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { fetchAdminOverview, onAdminCountsChanged, type AdminOverview } from "../api/admin";
import { IconArchive, IconCalendar, IconCheck, IconList, IconLogout, IconUser, IconUsers } from "./Icons";
import logo from '../tv-wettingen-logo.png';

interface NavItem {
  to: string;
  label: string;
  Icon: typeof IconArchive;
  end: boolean;
  /** Anzahl offener Aufgaben, die neben dem Eintrag angezeigt wird. */
  count?: number;
}

/** So oft wird nachgeschaut, ob neue Anträge oder Registrierungen eingegangen sind. */
const ADMIN_COUNTS_REFRESH_MS = 60_000;

export default function Layout() {
  const { isAdmin, logout, passwordResetRequired } = useAuth();
  const location = useLocation();
  const [overview, setOverview] = useState<AdminOverview | null>(null);

  const loadOverview = useCallback(() => {
    if (!isAdmin) {
      setOverview(null);
      return;
    }
    // Die Zähler sind ein Zusatz - schlägt das Laden fehl, bleibt die Navigation einfach ohne.
    fetchAdminOverview()
      .then(setOverview)
      .catch(() => undefined);
  }, [isAdmin]);

  // Beim Seitenwechsel, nach einer Entscheidung und regelmässig im Hintergrund.
  useEffect(() => {
    loadOverview();
  }, [loadOverview, location.pathname]);

  useEffect(() => {
    if (!isAdmin) return;
    const interval = window.setInterval(() => {
      if (document.visibilityState === "visible") loadOverview();
    }, ADMIN_COUNTS_REFRESH_MS);
    const unsubscribe = onAdminCountsChanged(loadOverview);
    return () => {
      window.clearInterval(interval);
      unsubscribe();
    };
  }, [isAdmin, loadOverview]);

  // Admins geben Anträge frei und verwalten Mitglieder; Mitglieder sehen den
  // Stand ihrer eigenen Anträge.
  const navItems: NavItem[] = [
    { to: "/", label: "Archiv", Icon: IconArchive, end: true },
    { to: "/events", label: "Events", Icon: IconCalendar, end: false },
    ...(isAdmin
      ? [
          { to: "/requests", label: "Anfragen", Icon: IconCheck, end: false, count: overview?.pendingRequests },
          { to: "/members", label: "Mitglieder", Icon: IconUsers, end: false, count: overview?.pendingRegistrations },
        ]
      : [{ to: "/my-requests", label: "Anträge", Icon: IconList, end: false }]),
    { to: "/profile", label: "Profil", Icon: IconUser, end: false },
  ];

  // Der Verlauf gehört zu den Anfragen und hat keinen eigenen Menüpunkt.
  const isCurrentSection = (to: string) => to === "/requests" && location.pathname === "/history";

  return (
    <div className="app-shell">
      <header className="topbar">
          <div className="topbar__brand"><img src={logo} alt="TV Wettingen Logo" />
          </div>
          <nav className="topbar__nav">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `topbar__link ${isActive || isCurrentSection(item.to) ? "is-active" : ""}`}
            >
              {item.label}
              {!!item.count && (
                <span className="nav-badge" aria-label={`${item.count} offen`}>
                  {item.count}
                </span>
              )}
            </NavLink>
          ))}
        </nav>
        <button type="button" className="topbar__logout" onClick={logout}>
          Abmelden
        </button>
      </header>

      <main className="app-content">
        {passwordResetRequired && location.pathname !== "/profile" && (
          <div className="notice-banner" role="status">
            <span>Du bist mit einem vorläufigen Passwort angemeldet. Bitte setze ein eigenes.</span>
            <Link to="/profile" className="btn btn--primary">
              Passwort ändern
            </Link>
          </div>
        )}
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="Hauptnavigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `tabbar__item ${isActive || isCurrentSection(item.to) ? "is-active" : ""}`}
          >
            <span className="tabbar__icon-wrap">
              <item.Icon className="tabbar__icon" />
              {!!item.count && (
                <span className="nav-badge nav-badge--floating" aria-label={`${item.count} offen`}>
                  {item.count}
                </span>
              )}
            </span>
            <span>{item.label}</span>
          </NavLink>
        ))}
        <button type="button" className="tabbar__item tabbar__item--button" onClick={logout}>
          <IconLogout className="tabbar__icon" />
          <span>Abmelden</span>
        </button>
      </nav>
    </div>
  );
}
