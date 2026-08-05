import { useState } from "react";
import EmptyState from "./EmptyState";

export default function ClientesView({ clientes, resumenes, onCreate, onRemove }) {
  const [query, setQuery] = useState("");
  const [adding, setAdding] = useState(false);
  const [nombre, setNombre] = useState("");
  const [telefono, setTelefono] = useState("");
  const [error, setError] = useState("");

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
    await onCreate({ nombre: nombre.trim(), telefono: telefono.trim() });
    setAdding(false);
    setNombre("");
    setTelefono("");
    setError("");
  }

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
                onChange={(e) => setTelefono(e.target.value)}
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
                  <td>{n}</td>
                  <td>
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
    </>
  );
}
