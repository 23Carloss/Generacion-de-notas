import { useState } from "react";
import TicketCard from "./TicketCard";
import EmptyState from "./EmptyState";

export default function TicketsView({ resumenes, onNavigate, onOpenTicket }) {
  const [query, setQuery] = useState("");
  const q = query.toLowerCase();

  const list = q
    ? resumenes.filter(
        (r) =>
          (r.cliente && r.cliente.nombre.toLowerCase().includes(q)) ||
          r.estado.toLowerCase().includes(q) ||
          (r.listaDispositivos || []).some((d) =>
            d.modeloDispositivo.toLowerCase().includes(q)
          )
      )
    : resumenes;

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Todos los tickets</p>
          <h1>Tickets de reparación</h1>
          <p>Busca por cliente, modelo de equipo o estado del ticket.</p>
        </div>
        <button className="btn btn-primary" onClick={() => onNavigate("nuevo")}>
          + Nuevo ticket
        </button>
      </div>

      <div className="toolbar">
        <input
          className="input"
          style={{ minWidth: 260 }}
          placeholder="Buscar cliente, equipo o estado…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {list.length ? (
        <div className="tag-grid">
          {list.map((r) => (
            <TicketCard key={r.id} resumen={r} onOpen={onOpenTicket} />
          ))}
        </div>
      ) : (
        <EmptyState title="Sin resultados" subtitle="Ningún ticket coincide con esa búsqueda." />
      )}
    </>
  );
}
