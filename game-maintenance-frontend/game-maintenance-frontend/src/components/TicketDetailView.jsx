import { useState } from "react";
import Badge from "./Badge";
import EmptyState from "./EmptyState";
import { ESTADOS, PLATAFORMAS, TIPOS_TRABAJO } from "../constants";
import { money, fecha, totalTicket } from "../utils";

export default function TicketDetailView({
  resumen,
  onBack,
  onUpdateEstado,
  onDelete,
}) {
  const [confirming, setConfirming] = useState(false);

  if (!resumen) {
    return (
      <EmptyState
        title="Ticket no encontrado"
        subtitle="Puede que ya se haya eliminado."
        actionLabel="Volver a tickets"
        onAction={onBack}
      />
    );
  }

  const total = totalTicket(resumen);

  return (
    <>
      <button className="back-link" onClick={onBack}>
        ← Volver a tickets
      </button>

      <div className="view-header">
        <div>
          <p className="eyebrow">Ticket #{resumen.id}</p>
          <h1>{resumen.cliente ? resumen.cliente.nombre : "Cliente"}</h1>
          <p>Recibido el {fecha(resumen.fechaCreacion)}</p>
        </div>
        <Badge estado={resumen.estado} large />
      </div>

      <div className="detail-grid">
        <div className="card">
          <div className="field">
            <label>Descripción del problema</label>
            <div>{resumen.descripcionProblema || "—"}</div>
          </div>
          <div className="field">
            <label>Comentarios del cliente</label>
            <div>{resumen.comentariosCliente || "—"}</div>
          </div>

          <div className="subblock">
            <h3>Dispositivos</h3>
            {(resumen.listaDispositivos || []).map((d) => (
              <div key={d.id} style={{ marginBottom: ".6rem" }}>
                <strong>{d.modeloDispositivo}</strong> · {PLATAFORMAS[d.plataforma] || d.plataforma}
                <div className="hint">{d.detallesDispositivo || ""}</div>
              </div>
            ))}
          </div>

          <div className="subblock">
            <h3>Trabajos realizados</h3>
            {resumen.listaTrabajos && resumen.listaTrabajos.length ? (
              <table>
                <tbody>
                  {resumen.listaTrabajos.map((t) => (
                    <tr key={t.id}>
                      <td>{TIPOS_TRABAJO[t.tipoTrabajo] || t.tipoTrabajo}</td>
                      <td style={{ textAlign: "right" }}>{money(t.precio)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="hint">Sin trabajos registrados todavía.</p>
            )}
            <div
              style={{
                textAlign: "right",
                marginTop: ".8rem",
                fontFamily: "var(--font-display)",
                fontWeight: 700,
              }}
            >
              Total: {money(total)}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="field">
            <label>Actualizar estado</label>
            <select
              value={resumen.estado}
              onChange={(e) => onUpdateEstado(resumen.id, e.target.value)}
            >
              {ESTADOS.map((e) => (
                <option key={e} value={e}>
                  {e}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>Teléfono de contacto</label>
            <div>{resumen.cliente ? resumen.cliente.telefono : "—"}</div>
          </div>

          <button
            className="btn btn-danger btn-small"
            style={{ width: "100%", justifyContent: "center" }}
            onClick={() => setConfirming(true)}
          >
            Eliminar ticket
          </button>

          {confirming && (
            <div className="confirm-inline">
              ¿Eliminar este ticket permanentemente?
              <button
                className="btn btn-danger btn-small"
                onClick={() => onDelete(resumen.id)}
              >
                Sí, eliminar
              </button>
              <button className="btn btn-ghost btn-small" onClick={() => setConfirming(false)}>
                Cancelar
              </button>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
