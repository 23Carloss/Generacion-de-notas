import { useState } from "react";
import { PLATAFORMAS, TIPOS_TRABAJO } from "../constants";

function blankDispositivo() {
  return { modeloDispositivo: "", detallesDispositivo: "", plataforma: "PLAYSTATION" };
}
function blankTrabajo() {
  return { tipoTrabajo: "DIAGNOSTICO", precio: "", nombrePieza: "", unidades: "", precioUnitario: "" };
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
    setTrabajos((prev) =>
      prev.map((t, idx) => {
        if (idx !== i) return t;
        if (field === "tipoTrabajo" && value !== "REPARACION") {
          return { ...t, tipoTrabajo: value, nombrePieza: "", unidades: "", precioUnitario: "" };
        }
        return { ...t, [field]: value };
      })
    );
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
      try {
        clienteRef = await onCreateCliente({
          nombre: nuevoNombre.trim(),
          telefono: nuevoTelefono.trim(),
        });
      } catch (e) {
        setError(e?.message || "No se pudo crear el cliente.");
        return;
      }
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

    const trabajosValidos = [];
    for (const trabajo of trabajos) {
      if (trabajo.tipoTrabajo === "REPARACION") {
        if (!trabajo.nombrePieza.trim() || trabajo.unidades === "" || trabajo.precioUnitario === "") {
          setError("En una reparación indica la pieza, las unidades y el precio por unidad.");
          return;
        }
        const unidades = Number(trabajo.unidades);
        const precioUnitario = Number(trabajo.precioUnitario);
        if (!Number.isInteger(unidades) || unidades < 1 || precioUnitario < 0) {
          setError("Las unidades deben ser al menos 1 y el precio por unidad no puede ser negativo.");
          return;
        }
        trabajosValidos.push({
          tipoTrabajo: trabajo.tipoTrabajo,
          nombrePieza: trabajo.nombrePieza.trim(),
          unidades,
          precioUnitario,
          precio: unidades * precioUnitario,
        });
      } else if (trabajo.precio !== "" && trabajo.precio != null) {
        const precio = Number(trabajo.precio);
        if (precio < 0) {
          setError("El precio del trabajo no puede ser negativo.");
          return;
        }
        trabajosValidos.push({ tipoTrabajo: trabajo.tipoTrabajo, precio });
      }
    }

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
                  inputMode="numeric"
                  maxLength={10}
                  onChange={(e) => setNuevoTelefono(e.target.value.replace(/\D/g, ""))}
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
            <div className="trabajo-editor" key={i}>
              <div className="repeat-row trabajo-row">
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
                {t.tipoTrabajo === "REPARACION" ? (
                  <div className="repair-price-note">El total se calcula con las unidades y el precio por unidad.</div>
                ) : (
                  <input
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Precio"
                    value={t.precio}
                    onChange={(e) => updateTrabajo(i, "precio", e.target.value)}
                  />
                )}
                <button
                  className="remove-x"
                  title="Quitar"
                  onClick={() => setTrabajos((prev) => prev.filter((_, idx) => idx !== i))}
                >
                  ✕
                </button>
              </div>
              {t.tipoTrabajo === "REPARACION" && (
                <div className="repair-fields">
                  <input
                    className="input"
                    placeholder="Nombre de la pieza"
                    value={t.nombrePieza}
                    onChange={(e) => updateTrabajo(i, "nombrePieza", e.target.value)}
                  />
                  <input
                    className="input"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="Unidades"
                    value={t.unidades}
                    onChange={(e) => updateTrabajo(i, "unidades", e.target.value)}
                  />
                  <input
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Precio por unidad"
                    value={t.precioUnitario}
                    onChange={(e) => updateTrabajo(i, "precioUnitario", e.target.value)}
                  />
                </div>
              )}
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
