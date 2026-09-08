import { CONFIG } from "../api";
import { useEffect, useRef, useState } from "react";
import { ROLES } from "../constants";

const NAV_ADMIN = [
  { key: "dashboard", label: "Panel" },
  { key: "nuevo", label: "Nuevo ticket" },
  { key: "tickets", label: "Tickets" },
  { key: "clientes", label: "Clientes" },
  { key: "resueltos", label: "Trabajos resueltos" },
];

const NAV_USUARIO = [
  { key: "resueltos", label: "Trabajos resueltos" },
  { key: "tickets", label: "Mis tickets" },
  { key: "perfil", label: "Mi perfil" },
];

export default function Sidebar({ route, onNavigate, usuario, esAdmin, onLogout }) {
  const nav = esAdmin ? NAV_ADMIN : NAV_USUARIO;
  const [open, setOpen] = useState(false);
  const dialog = useRef(null);
  const trigger = useRef(null);

  useEffect(() => {
    const element = dialog.current;
    if (!open) { element.close(); return; }
    element.showModal();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = previousOverflow; element.close(); };
  }, [open]);

  function close() { setOpen(false); trigger.current?.focus(); }
  function go(key) { close(); onNavigate(key); }

  return (
    <>
    <a className="skip-link" href="#contenido" onClick={(e) => { e.preventDefault(); document.getElementById("contenido")?.focus(); }}>Saltar al contenido</a>
    <header className="app-header">
      <button ref={trigger} type="button" className="btn btn-ghost menu-toggle"
        aria-label="Abrir menú" aria-expanded={open} aria-controls="navigation-drawer" onClick={() => setOpen(true)}>
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="M3 6h18M3 12h18M3 18h18" /></svg>
        <span>Menú</span>
      </button>
      <span className="header-brand">Game Maintenance</span>
      <span className="session-name">{usuario.nombre}</span>
    </header>
    <dialog ref={dialog} id="navigation-drawer" className="navigation-drawer" aria-label="Menú principal"
      onKeyDown={(e) => {
        if (e.key !== "Tab") return;
        const buttons = [...e.currentTarget.querySelectorAll('button:not([disabled])')];
        const first = buttons[0], last = buttons.at(-1);
        if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last?.focus(); }
        else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first?.focus(); }
      }}
      onCancel={close} onClose={() => setOpen(false)} onClick={(e) => {
        const box = e.currentTarget.getBoundingClientRect();
        if (e.target === e.currentTarget && (e.clientX > box.right || e.clientX < box.left || e.clientY < box.top || e.clientY > box.bottom)) close();
      }}>
    <aside className="sidebar">
      <button type="button" className="btn btn-ghost drawer-close" onClick={close} aria-label="Cerrar menú">✕ <span>Cerrar</span></button>
      <div className="brand">
        <div className="line1">Taller de consolas</div>
        <div className="line2">Game Maintenance</div>
        <div className="line3">Panel de servicio</div>
      </div>
      <nav className="nav" aria-label="Apartados">
        {nav.map((n) => (
          <button
            key={n.key}
            className={
              "nav-item " +
              (route === n.key || (route === "ticket" && n.key === "tickets") ? "active" : "")
            }
            aria-current={route === n.key || (route === "ticket" && n.key === "tickets") ? "page" : undefined}
            onClick={() => go(n.key)}
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
              onClick={() => { close(); onLogout(); }}
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
    </dialog>
    </>
  );
}
