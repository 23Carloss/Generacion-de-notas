import { useEffect, useState } from "react";
import { Api } from "../api";
import EmptyState from "./EmptyState";

const PAGE_SIZE = 20;

function fechaHora(valor) {
  if (!valor) return "—";
  return new Intl.DateTimeFormat("es-MX", { dateStyle: "medium", timeStyle: "short" }).format(new Date(valor));
}

export default function NotificacionesView({ onOpen, onCountChange }) {
  const [items, setItems] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [total, setTotal] = useState(0);
  const [filtro, setFiltro] = useState("todas");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [abriendo, setAbriendo] = useState(null);

  async function cargar(nextPage = pagina, nextFilter = filtro) {
    setLoading(true);
    setError("");
    try {
      const unread = nextFilter === "todas" ? null : nextFilter === "no-leidas";
      const data = await Api.notificaciones.list({ page: nextPage, size: PAGE_SIZE, unread });
      setItems(data.notificaciones || []);
      setTotal(data.total || 0);
      onCountChange(data.noLeidas || 0);
    } catch (e) {
      setError(e?.message || "No se pudieron cargar las notificaciones.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { cargar(); }, [pagina, filtro]); // eslint-disable-line react-hooks/exhaustive-deps

  async function abrir(notificacion) {
    setAbriendo(notificacion.id);
    setError("");
    try {
      if (!notificacion.leida) {
        await Api.notificaciones.markRead(notificacion.id);
        setItems((prev) => prev.map((n) => n.id === notificacion.id ? { ...n, leida: true } : n));
        onCountChange((count) => Math.max(0, count - 1));
      }
      onOpen(notificacion);
    } catch (e) {
      setError(e?.message || "No se pudo abrir la notificación.");
    } finally {
      setAbriendo(null);
    }
  }

  async function marcarTodas() {
    setError("");
    try {
      await Api.notificaciones.markAllRead();
      onCountChange(0);
      if (filtro === "no-leidas") {
        setPagina(0);
        await cargar(0, "no-leidas");
      } else {
        setItems((prev) => prev.map((n) => ({ ...n, leida: true })));
      }
    } catch (e) {
      setError(e?.message || "No se pudieron marcar las notificaciones.");
    }
  }

  const paginas = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Actividad</p>
          <h1>Notificaciones</h1>
          <p>Actualizaciones de tus tickets y eventos que requieren tu atención.</p>
        </div>
        <button className="btn btn-ghost" type="button" onClick={marcarTodas} disabled={loading || !items.some((n) => !n.leida)}>
          Marcar todas como leídas
        </button>
      </div>

      <div className="notification-toolbar" role="group" aria-label="Filtro de notificaciones">
        <button type="button" className={filtro === "todas" ? "active" : ""} onClick={() => { setPagina(0); setFiltro("todas"); }}>Todas</button>
        <button type="button" className={filtro === "no-leidas" ? "active" : ""} onClick={() => { setPagina(0); setFiltro("no-leidas"); }}>No leídas</button>
        <button type="button" className={filtro === "leidas" ? "active" : ""} onClick={() => { setPagina(0); setFiltro("leidas"); }}>Leídas</button>
      </div>

      {error && <div className="card notification-error" role="alert"><p>{error}</p><button className="btn btn-ghost btn-small" onClick={() => cargar()}>Reintentar</button></div>}
      {loading ? (
        <p className="hint" role="status">Cargando notificaciones…</p>
      ) : items.length ? (
        <div className="notification-list">
          {items.map((n) => (
            <button key={n.id} type="button" className={`notification-item ${n.leida ? "is-read" : "is-unread"}`}
              disabled={abriendo === n.id} onClick={() => abrir(n)}>
              <span className="notification-status" aria-hidden="true" />
              <span className="notification-copy">
                <strong>{n.titulo}</strong>
                <span>{n.mensaje}</span>
                <time dateTime={n.fechaCreacion}>{fechaHora(n.fechaCreacion)}</time>
              </span>
              {n.ticketId && <span className="notification-link">Ticket #{n.ticketId} →</span>}
            </button>
          ))}
        </div>
      ) : (
        <EmptyState title="No tienes notificaciones" subtitle={filtro === "no-leidas" ? "Ya revisaste todas tus notificaciones." : "No hay elementos para este filtro."} />
      )}

      {!loading && total > PAGE_SIZE && <div className="notification-pagination">
        <button className="btn btn-ghost btn-small" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>Anterior</button>
        <span>Página {pagina + 1} de {paginas}</span>
        <button className="btn btn-ghost btn-small" disabled={pagina + 1 >= paginas} onClick={() => setPagina((p) => p + 1)}>Siguiente</button>
      </div>}
    </>
  );
}
