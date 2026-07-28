import TicketCard from "./TicketCard";
import EmptyState from "./EmptyState";
import { money, totalTicket } from "../utils";

export default function DashboardView({ resumenes, onNavigate, onOpenTicket }) {
  const total = resumenes.length;
  const activos = resumenes.filter((r) => r.estado !== "Entregado").length;
  const listos = resumenes.filter((r) => r.estado === "Listo para entrega").length;
  const ingresos = resumenes.reduce((s, r) => s + totalTicket(r), 0);
  const recientes = resumenes.slice(0, 3);

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Vista general</p>
          <h1>Panel de servicio</h1>
          <p>Estado actual del taller: tickets activos, listos para entrega e ingresos estimados.</p>
        </div>
        <button className="btn btn-primary" onClick={() => onNavigate("nuevo")}>
          + Nuevo ticket
        </button>
      </div>

      <div className="stat-grid">
        <StatCard num={total} lbl="Tickets totales" />
        <StatCard num={activos} lbl="En proceso" accent />
        <StatCard num={listos} lbl="Listos para entrega" />
        <StatCard num={money(ingresos)} lbl="Ingresos estimados" />
      </div>

      <div className="view-header" style={{ marginBottom: "1rem" }}>
        <h1 style={{ fontSize: "1.05rem" }}>Tickets recientes</h1>
        <button className="btn btn-ghost btn-small" onClick={() => onNavigate("tickets")}>
          Ver todos →
        </button>
      </div>

      {recientes.length ? (
        <div className="tag-grid">
          {recientes.map((r) => (
            <TicketCard key={r.id} resumen={r} onOpen={onOpenTicket} />
          ))}
        </div>
      ) : (
        <EmptyState
          title="Aún no hay tickets"
          subtitle="Registra el primer ticket de reparación para empezar."
          actionLabel="+ Nuevo ticket"
          onAction={() => onNavigate("nuevo")}
        />
      )}
    </>
  );
}

function StatCard({ num, lbl, accent }) {
  return (
    <div className={"stat-card" + (accent ? " accent" : "")}>
      <div className="num">{num}</div>
      <div className="lbl">{lbl}</div>
    </div>
  );
}
