import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { IconArchive, IconCalendar, IconCheck, IconLogout, IconUser } from "./Icons";

const navItems = [
  { to: "/", label: "Archiv", Icon: IconArchive, end: true },
  { to: "/events", label: "Events", Icon: IconCalendar, end: false },
];

export default function Layout() {
  const { isAdmin, logout } = useAuth();

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="topbar__brand">Materialarchiv</div>
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
          {isAdmin && (
            <NavLink
              to="/requests"
              className={({ isActive }) => `topbar__link ${isActive ? "is-active" : ""}`}
            >
              Anfragen
            </NavLink>
          )}
          <NavLink to="/profile" className={({ isActive }) => `topbar__link ${isActive ? "is-active" : ""}`}>
            Profil
          </NavLink>
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
        {isAdmin && (
          <NavLink
            to="/requests"
            className={({ isActive }) => `tabbar__item ${isActive ? "is-active" : ""}`}
          >
            <IconCheck className="tabbar__icon" />
            <span>Anfragen</span>
          </NavLink>
        )}
        <NavLink to="/profile" className={({ isActive }) => `tabbar__item ${isActive ? "is-active" : ""}`}>
          <IconUser className="tabbar__icon" />
          <span>Profil</span>
        </NavLink>
        <button type="button" className="tabbar__item tabbar__item--button" onClick={logout}>
          <IconLogout className="tabbar__icon" />
          <span>Abmelden</span>
        </button>
      </nav>
    </div>
  );
}
