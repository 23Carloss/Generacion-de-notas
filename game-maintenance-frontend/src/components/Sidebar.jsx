import { CONFIG } from "../api";

const NAV = [
  { key: "dashboard", label: "Panel" },
  { key: "nuevo", label: "Nuevo ticket" },
  { key: "tickets", label: "Tickets" },
  { key: "clientes", label: "Clientes" },
];

export default function Sidebar({ route, onNavigate }) {
  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="line1">Taller de consolas</div>
        <div className="line2">Game Maintenance</div>
        <div className="line3">Panel de servicio</div>
      </div>
      <nav className="nav">
        {NAV.map((n) => (
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
        <span className="mode-pill">
          <span className="dot" />
          {CONFIG.MOCK ? "modo demo" : "conectado a API"}
        </span>
        <br />
        Datos de ejemplo en memoria.
        <br />
        Ver README para conectar el backend real.
      </div>
    </aside>
  );
}
