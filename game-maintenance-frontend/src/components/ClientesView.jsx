import { useState } from "react";
import EmptyState from "./EmptyState";
import { ROLES } from "../constants";
import PhoneField from "./PhoneField";
import { joinPhone, splitPhone } from "../phone";
import { LIMITS } from "../formValidation";

export default function ClientesView({ clientes, resumenes, ticketsError, onRetry, onCreate, onUpdate, onRemove, onOpenTicket }) {
  const [query, setQuery] = useState("");
  const [adding, setAdding] = useState(false);
  const [editing, setEditing] = useState(null);
  const [clienteSeleccionadoId, setClienteSeleccionadoId] = useState(null);

  const q = query.toLowerCase();
  const list = q
    ? clientes.filter(
        (c) => c.nombre.toLowerCase().includes(q) || (c.telefono || "").includes(q)
      )
    : clientes;

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
        <button className="btn btn-primary" onClick={() => { setEditing(null); setAdding((v) => !v); }}>
          + Nuevo cliente
        </button>
      </div>

      {(adding || editing) && (
        <ClienteForm key={editing?.id ?? "nuevo"} cliente={editing}
          onCancel={() => { setAdding(false); setEditing(null); }}
          onSave={async (data) => {
            if (editing) await onUpdate(editing.id, data);
            else await onCreate(data);
            setAdding(false);
            setEditing(null);
          }} />
      )}

      <div className="toolbar">
        <input
          className="input"
          style={{ minWidth: 260 }}
          placeholder="Buscar por nombre o teléfono…"
          aria-label="Buscar clientes"
          maxLength={LIMITS.search}
          value={query}
          onChange={(e) => setQuery(e.target.value.slice(0, LIMITS.search))}
        />
      </div>

      {list.length ? (
        <div className="table-scroll" role="region" aria-label="Lista de clientes" tabIndex={0}><table>
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
                  <td>{ticketsError ? "No disponible" : n}</td>
                  <td>
                    <button
                      className="btn btn-ghost btn-small"
                      onClick={() => setClienteSeleccionadoId(c.id)}
                    >
                      Ver información
                    </button>{" "}
                    <button className="btn btn-ghost btn-small" onClick={() => { setAdding(false); setEditing(c); }}>
                      Editar
                    </button>{" "}
                    <button className="btn btn-ghost btn-small" onClick={() => onRemove(c.id)}>
                      Eliminar
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table></div>
      ) : (
        <EmptyState
          title="Sin clientes"
          subtitle="Registra tu primer cliente para comenzar a crear tickets."
        />
      )}

      {ticketsError && <div className="card" role="alert">
        <p>Los clientes se cargaron, pero sus tickets no están disponibles temporalmente.</p>
        <button className="btn btn-ghost" onClick={onRetry}>Reintentar carga</button>
      </div>}

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
            <label>Tickets del cliente{ticketsError ? "" : ` (${ticketsCliente.length})`}</label>
            {ticketsError ? <p className="hint">No se pudo consultar el historial. Esto no significa que no tenga tickets.</p> : ticketsCliente.length ? (
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

function ClienteForm({ cliente, onSave, onCancel }) {
  const [nombre, setNombre] = useState(cliente?.nombre || "");
  const [phone, setPhone] = useState(() => splitPhone(cliente?.telefono || ""));
  const [correo, setCorreo] = useState(cliente?.correo || "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function save(e) {
    e.preventDefault();
    setError("");
    if (!nombre.trim() || nombre.trim().length > LIMITS.clientName) {
      setError("El nombre debe tener entre 1 y 100 caracteres.");
      return;
    }
    setSaving(true);
    try {
      const original = splitPhone(cliente?.telefono || "");
      const unchanged = cliente && phone.dialCode === original.dialCode && phone.number === original.number;
      const telefono = unchanged ? cliente.telefono : joinPhone(phone);
      await onSave({ nombre: nombre.trim(), telefono, ...(cliente ? { correo: correo.trim() } : {}) });
    } catch (err) {
      setError(err?.message || "No se pudo guardar el cliente.");
    } finally { setSaving(false); }
  }

  return (
    <form className="card" style={{ marginBottom: "1.4rem" }} onSubmit={save}>
      <h2>{cliente ? "Editar cliente" : "Nuevo cliente"}</h2>
      <div className="row2">
        <div className="field">
          <label htmlFor="cliente-nombre">Nombre</label>
          <input id="cliente-nombre" className="input" placeholder="Nombre completo" value={nombre}
            required maxLength={LIMITS.clientName} disabled={saving}
            onChange={(e) => setNombre(e.target.value.slice(0, LIMITS.clientName))} />
          <span className="hint">{nombre.length}/100 caracteres</span>
        </div>
        <PhoneField value={phone} onChange={setPhone} disabled={saving} />
      </div>
      {cliente && <div className="field">
        <label htmlFor="cliente-correo">Correo</label>
        <input id="cliente-correo" className="input" type="email" maxLength={254} value={correo}
          disabled={saving} onChange={(e) => setCorreo(e.target.value)} />
        <span className="hint">Si tiene cuenta, cambiar el correo cambiará su dirección de inicio de sesión.</span>
      </div>}
      {error && <div className="error-text" role="alert">{error}</div>}
      <div className="toolbar">
        <button className="btn btn-primary" type="submit" disabled={saving}>{saving ? "Guardando…" : "Guardar cliente"}</button>
        <button className="btn btn-ghost" type="button" disabled={saving} onClick={onCancel}>Cancelar</button>
      </div>
    </form>
  );
}
