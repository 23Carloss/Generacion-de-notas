import { CONFIG } from "../api";
import { ROLES } from "../constants";

const NAV_ADMIN = [
  { key: "dashboard", label: "Panel" },
  { key: "nuevo", label: "Nuevo ticket" },
  { key: "tickets", label: "Tickets" },
  { key: "clientes", label: "Clientes" },
];

const NAV_USUARIO = [
  { key: "dashboard", label: "Panel" },
  { key: "tickets", label: "Mis tickets" },
  { key: "perfil", label: "Mi perfil" },
];

export default function Sidebar({ route, onNavigate, usuario, esAdmin, onLogout }) {
  const nav = esAdmin ? NAV_ADMIN : NAV_USUARIO;

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="line1">Taller de consolas</div>
        <div className="line2">Game Maintenance</div>
        <div className="line3">Panel de servicio</div>
      </div>
      <nav className="nav">
        {nav.map((n) => (
          <button
            key={n.key}
            className={
              "nav-item " +
              (route === n.key || (route === "ticket" && n.key === "tickets") ? "active" : "")
            }
            onClick={() => onNavigate(n.key)}
          >
            <span className="dot" />
            {n.label}
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        {usuario && (
          <div className="user-box">
            <div className="user-name">{usuario.nombre}</div>
            <div className="user-rol">{ROLES[usuario.rol] || usuario.rol}</div>
            <button
              className="btn btn-ghost btn-small"
              style={{ width: "100%", marginTop: ".5rem" }}
              onClick={onLogout}
            >
              Cerrar sesión
            </button>
          </div>
        )}
        <span className="mode-pill" style={{ marginTop: ".8rem" }}>
          <span className="dot" />
          {CONFIG.MOCK ? "modo demo" : "conectado a API"}
        </span>
      </div>
    </aside>
  );
}
