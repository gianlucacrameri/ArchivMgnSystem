import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { IconArchive, IconCalendar, IconCheck, IconList, IconLogout, IconUser, IconUsers } from "./Icons";
import logo from '../tv-wettingen-logo.png';

const commonItems = [
  { to: "/", label: "Archiv", Icon: IconArchive, end: true },
  { to: "/events", label: "Events", Icon: IconCalendar, end: false },
];

// Admins geben Anträge frei und verwalten Mitglieder; Mitglieder sehen den
// Stand ihrer eigenen Anträge.
const adminItems = [
  { to: "/requests", label: "Anfragen", Icon: IconCheck, end: false },
  { to: "/members", label: "Mitglieder", Icon: IconUsers, end: false },
];
const memberItems = [{ to: "/my-requests", label: "Anträge", Icon: IconList, end: false }];

const profileItem = { to: "/profile", label: "Profil", Icon: IconUser, end: false };

export default function Layout() {
  const { isAdmin, logout } = useAuth();
  const navItems = [...commonItems, ...(isAdmin ? adminItems : memberItems), profileItem];

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
              className={({ isActive }) => `topbar__link ${isActive ? "is-active" : ""}`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <button type="button" className="topbar__logout" onClick={logout}>
          Abmelden
        </button>
      </header>

      <main className="app-content">
        <Outlet />
      </main>

      <nav className="tabbar" aria-label="Hauptnavigation">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) => `tabbar__item ${isActive ? "is-active" : ""}`}
          >
            <item.Icon className="tabbar__icon" />
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
