import { useState } from "react";
import EmptyState from "./EmptyState";
import { ROLES } from "../constants";

export default function ClientesView({ clientes, resumenes, onCreate, onRemove, onOpenTicket }) {
  const [query, setQuery] = useState("");
  const [adding, setAdding] = useState(false);
  const [nombre, setNombre] = useState("");
  const [telefono, setTelefono] = useState("");
  const [error, setError] = useState("");
  const [clienteSeleccionadoId, setClienteSeleccionadoId] = useState(null);

  const q = query.toLowerCase();
  const list = q
    ? clientes.filter(
        (c) => c.nombre.toLowerCase().includes(q) || (c.telefono || "").includes(q)
      )
    : clientes;

  async function handleSave() {
    if (!nombre.trim() || !telefono.trim()) {
      setError("Nombre y teléfono son obligatorios.");
      return;
    }
    try {
      await onCreate({ nombre: nombre.trim(), telefono: telefono.trim() });
      setAdding(false);
      setNombre("");
      setTelefono("");
      setError("");
    } catch (e) {
      setError(e?.message || "No se pudo guardar el cliente.");
    }
  }

  const clienteSeleccionado = clientes.find((c) => c.id === clienteSeleccionadoId);
  const ticketsCliente = clienteSeleccionado
    ? resumenes.filter((r) => r.cliente?.id === clienteSeleccionado.id)
    : [];

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Directorio</p>
          <h1>Clientes</h1>
          <p>Datos de contacto de quienes han dejado equipo en el taller.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setAdding((v) => !v)}>
          + Nuevo cliente
        </button>
      </div>

      {adding && (
        <div className="card" style={{ marginBottom: "1.4rem" }}>
          <div className="row2">
            <div className="field">
              <label>Nombre</label>
              <input
                className="input"
                placeholder="Nombre completo"
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
              />
            </div>
            <div className="field">
              <label>Teléfono</label>
              <input
                className="input"
                placeholder="10 dígitos"
                value={telefono}
                inputMode="numeric"
                maxLength={10}
                onChange={(e) => setTelefono(e.target.value.replace(/\D/g, ""))}
              />
            </div>
          </div>
          {error && <div className="error-text">{error}</div>}
          <button className="btn btn-primary" onClick={handleSave}>
            Guardar cliente
          </button>
        </div>
      )}

      <div className="toolbar">
        <input
          className="input"
          style={{ minWidth: 260 }}
          placeholder="Buscar por nombre o teléfono…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
      </div>

      {list.length ? (
        <table>
          <thead>
            <tr>
              <th>Nombre</th>
              <th>Teléfono</th>
              <th>Rol</th>
              <th>Tickets</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {list.map((c) => {
              const n = resumenes.filter((r) => r.cliente && r.cliente.id === c.id).length;
              return (
                <tr key={c.id}>
                  <td>{c.nombre}</td>
                  <td>{c.telefono}</td>
                  <td>{ROLES[c.rol] || "—"}</td>
                  <td>{n}</td>
                  <td>
                    <button
                      className="btn btn-ghost btn-small"
                      onClick={() => setClienteSeleccionadoId(c.id)}
                    >
                      Ver información
                    </button>{" "}
                    <button className="btn btn-ghost btn-small" onClick={() => onRemove(c.id)}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      ) : (
        <EmptyState
          title="Sin clientes"
          subtitle="Registra tu primer cliente para comenzar a crear tickets."
        />
      )}

      {clienteSeleccionado && (
        <section className="card client-detail" aria-live="polite">
          <div className="client-detail-header">
            <div>
              <p className="eyebrow">Información del cliente</p>
              <h2>{clienteSeleccionado.nombre}</h2>
            </div>
            <button
              className="remove-x"
              type="button"
              title="Cerrar información del cliente"
              aria-label="Cerrar información del cliente"
              onClick={() => setClienteSeleccionadoId(null)}
            >
              ✕
            </button>
          </div>
          <div className="row2">
            <div className="field">
              <label>Teléfono</label>
              <div>{clienteSeleccionado.telefono || "—"}</div>
            </div>
            <div className="field">
              <label>Correo</label>
              <div>{clienteSeleccionado.correo || "Sin cuenta registrada"}</div>
            </div>
          </div>
          <div className="field">
            <label>Tickets del cliente ({ticketsCliente.length})</label>
            {ticketsCliente.length ? (
              <div className="client-ticket-list">
                {ticketsCliente.map((ticket) => (
                  <button
                    className="client-ticket"
                    key={ticket.id}
                    type="button"
                    onClick={() => onOpenTicket(ticket.id)}
                  >
                    <span>Ticket #{ticket.id}</span>
                    <span>{ticket.estado}</span>
                  </button>
                ))}
              </div>
            ) : (
              <p className="hint">Este cliente aún no tiene tickets registrados.</p>
            )}
          </div>
        </section>
      )}
    </>
  );
}
