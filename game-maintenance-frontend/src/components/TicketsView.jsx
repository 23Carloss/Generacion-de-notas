import { useState } from "react";
import TicketCard from "./TicketCard";
import EmptyState from "./EmptyState";

export default function TicketsView({ resumenes, onNavigate, onOpenTicket, esAdmin }) {
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
          <p className="eyebrow">{esAdmin ? "Todos los tickets" : "Tus tickets"}</p>
          <h1>{esAdmin ? "Tickets de reparación" : "Mis tickets"}</h1>
          <p>
            {esAdmin
              ? "Busca por cliente, modelo de equipo o estado del ticket."
              : "Busca entre tus tickets por equipo o estado."}
          </p>
        </div>
        {esAdmin && (
          <button className="btn btn-primary" onClick={() => onNavigate("nuevo")}>
            + Nuevo ticket
          </button>
        )}
      </div>

      <div className="toolbar">
        <input
          className="input"
          style={{ maxWidth: 460 }}
          placeholder={esAdmin ? "Buscar cliente, equipo o estado…" : "Buscar equipo o estado…"}
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
        <EmptyState
          title="Sin resultados"
          subtitle={
            resumenes.length
              ? "Ningún ticket coincide con esa búsqueda."
              : esAdmin
              ? "Todavía no se ha registrado ningún ticket."
              : "Todavía no tienes tickets registrados."
          }
        />
      )}
    </>
  );
}
