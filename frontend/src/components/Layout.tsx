import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const navItems = [
  { to: "/", label: "Archiv", icon: "📦", end: true },
  { to: "/events", label: "Events", icon: "📅", end: false },
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
            <span className="tabbar__icon" aria-hidden="true">
              {item.icon}
            </span>
            <span>{item.label}</span>
          </NavLink>
        ))}
        {isAdmin && (
          <NavLink
            to="/requests"
            className={({ isActive }) => `tabbar__item ${isActive ? "is-active" : ""}`}
          >
            <span className="tabbar__icon" aria-hidden="true">
              ✅
            </span>
            <span>Anfragen</span>
          </NavLink>
        )}
        <button type="button" className="tabbar__item tabbar__item--button" onClick={logout}>
          <span className="tabbar__icon" aria-hidden="true">
            🚪
          </span>
          <span>Abmelden</span>
        </button>
      </nav>
    </div>
  );
}
