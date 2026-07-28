import { useState } from "react";
import { PLATAFORMAS, TIPOS_TRABAJO } from "../constants";

function blankDispositivo() {
  return { modeloDispositivo: "", detallesDispositivo: "", plataforma: "PLAYSTATION" };
}
function blankTrabajo() {
  return { tipoTrabajo: "DIAGNOSTICO", precio: "" };
}

export default function NuevoTicketView({ clientes, onCreateCliente, onCreateTicket }) {
  const [clienteMode, setClienteMode] = useState(clientes.length ? "existente" : "nuevo");
  const [clienteId, setClienteId] = useState(clientes[0] ? clientes[0].id : null);
  const [nuevoNombre, setNuevoNombre] = useState("");
  const [nuevoTelefono, setNuevoTelefono] = useState("");
  const [dispositivos, setDispositivos] = useState([blankDispositivo()]);
  const [trabajos, setTrabajos] = useState([]);
  const [descripcionProblema, setDescripcionProblema] = useState("");
  const [comentariosCliente, setComentariosCliente] = useState("");
  const [error, setError] = useState("");

  function updateDispositivo(i, field, value) {
    setDispositivos((prev) => prev.map((d, idx) => (idx === i ? { ...d, [field]: value } : d)));
  }
  function updateTrabajo(i, field, value) {
    setTrabajos((prev) => prev.map((t, idx) => (idx === i ? { ...t, [field]: value } : t)));
  }

  async function handleSubmit() {
    setError("");
    let clienteRef = null;

    if (clienteMode === "existente") {
      clienteRef = clientes.find((c) => c.id === clienteId);
      if (!clienteRef) {
        setError("Selecciona un cliente.");
        return;
      }
    } else {
      if (!nuevoNombre.trim() || !nuevoTelefono.trim()) {
        setError("Nombre y teléfono del cliente nuevo son obligatorios.");
        return;
      }
      clienteRef = await onCreateCliente({
        nombre: nuevoNombre.trim(),
        telefono: nuevoTelefono.trim(),
      });
    }

    const dispositivosValidos = dispositivos.filter((d) => d.modeloDispositivo.trim());
    if (!dispositivosValidos.length) {
      setError("Agrega al menos un dispositivo con su modelo.");
      return;
    }
    if (!descripcionProblema.trim()) {
      setError("Describe el problema reportado.");
      return;
    }

    const trabajosValidos = trabajos
      .filter((t) => t.precio !== "" && t.precio != null)
      .map((t) => ({ tipoTrabajo: t.tipoTrabajo, precio: Number(t.precio) }));

    await onCreateTicket({
      cliente: clienteRef,
      listaDispositivos: dispositivosValidos,
      listaTrabajos: trabajosValidos,
      descripcionProblema: descripcionProblema.trim(),
      comentariosCliente: comentariosCliente.trim(),
    });
  }

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Nuevo ingreso</p>
          <h1>Crear ticket de reparación</h1>
          <p>Registra al cliente, el equipo que trae y el problema reportado.</p>
        </div>
      </div>

      <div className="card">
        <div className="subblock">
          <h3>Cliente</h3>
          <div className="toolbar" style={{ marginBottom: ".9rem" }}>
            <button
              className={"btn btn-small " + (clienteMode === "existente" ? "btn-primary" : "btn-ghost")}
              onClick={() => setClienteMode("existente")}
            >
              Cliente existente
            </button>
            <button
              className={"btn btn-small " + (clienteMode === "nuevo" ? "btn-primary" : "btn-ghost")}
              onClick={() => setClienteMode("nuevo")}
            >
              Cliente nuevo
            </button>
          </div>

          {clienteMode === "existente" ? (
            <div className="field">
              <label>Selecciona cliente</label>
              <select value={clienteId ?? ""} onChange={(e) => setClienteId(Number(e.target.value))}>
                {clientes.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nombre} · {c.telefono}
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div className="row2">
              <div className="field">
                <label>Nombre</label>
                <input
                  className="input"
                  placeholder="Nombre completo"
                  value={nuevoNombre}
                  onChange={(e) => setNuevoNombre(e.target.value)}
                />
              </div>
              <div className="field">
                <label>Teléfono</label>
                <input
                  className="input"
                  placeholder="10 dígitos"
                  value={nuevoTelefono}
                  onChange={(e) => setNuevoTelefono(e.target.value)}
                />
              </div>
            </div>
          )}
        </div>

        <div className="subblock">
          <h3>Dispositivos</h3>
          {dispositivos.map((d, i) => (
            <div className="repeat-row" key={i}>
              <input
                className="input"
                placeholder="Modelo (ej. PS4 Slim)"
                value={d.modeloDispositivo}
                onChange={(e) => updateDispositivo(i, "modeloDispositivo", e.target.value)}
              />
              <input
                className="input"
                placeholder="Detalles / accesorios / daños visibles"
                value={d.detallesDispositivo}
                onChange={(e) => updateDispositivo(i, "detallesDispositivo", e.target.value)}
              />
              <select
                value={d.plataforma}
                onChange={(e) => updateDispositivo(i, "plataforma", e.target.value)}
              >
                {Object.keys(PLATAFORMAS).map((p) => (
                  <option key={p} value={p}>
                    {PLATAFORMAS[p]}
                  </option>
                ))}
              </select>
              <button
                className="remove-x"
                title="Quitar"
                onClick={() => setDispositivos((prev) => prev.filter((_, idx) => idx !== i))}
              >
                ✕
              </button>
            </div>
          ))}
          <button
            className="btn btn-ghost btn-small"
            onClick={() => setDispositivos((prev) => [...prev, blankDispositivo()])}
          >
            + Agregar dispositivo
          </button>
        </div>

        <div className="row2">
          <div className="field">
            <label>Descripción del problema</label>
            <textarea
              className="input"
              rows={3}
              placeholder="Qué reporta el cliente…"
              value={descripcionProblema}
              onChange={(e) => setDescripcionProblema(e.target.value)}
            />
          </div>
          <div className="field">
            <label>Comentarios del cliente</label>
            <textarea
              className="input"
              rows={3}
              placeholder="Detalles adicionales, urgencia, etc."
              value={comentariosCliente}
              onChange={(e) => setComentariosCliente(e.target.value)}
            />
          </div>
        </div>

        <div className="subblock">
          <h3>Trabajos (opcional al ingreso)</h3>
          {trabajos.map((t, i) => (
            <div className="repeat-row trabajo-row" key={i}>
              <select
                value={t.tipoTrabajo}
                onChange={(e) => updateTrabajo(i, "tipoTrabajo", e.target.value)}
              >
                {Object.keys(TIPOS_TRABAJO).map((k) => (
                  <option key={k} value={k}>
                    {TIPOS_TRABAJO[k]}
                  </option>
                ))}
              </select>
              <input
                className="input"
                type="number"
                min="0"
                step="0.01"
                placeholder="Precio"
                value={t.precio}
                onChange={(e) => updateTrabajo(i, "precio", e.target.value)}
              />
              <button
                className="remove-x"
                title="Quitar"
                onClick={() => setTrabajos((prev) => prev.filter((_, idx) => idx !== i))}
              >
                ✕
              </button>
            </div>
          ))}
          <button
            className="btn btn-ghost btn-small"
            onClick={() => setTrabajos((prev) => [...prev, blankTrabajo()])}
          >
            + Agregar trabajo
          </button>
        </div>

        {error && <div className="error-text">{error}</div>}
        <button className="btn btn-primary" onClick={handleSubmit}>
          Crear ticket
        </button>
      </div>
    </>
  );
}
