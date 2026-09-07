import { useState } from "react";
import { PLATAFORMAS, TIPOS_TRABAJO } from "../constants";
import Select from "./Select";
import PhoneField from "./PhoneField";
import { joinPhone } from "../phone";
import { LIMITS, prepareWorks, validateDevices } from "../formValidation";

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
  const [nuevoTelefono, setNuevoTelefono] = useState({ dialCode: "52", number: "" });
  const [guardando, setGuardando] = useState(false);
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
    const dispositivosValidos = dispositivos.filter((d) => d.modeloDispositivo.trim());
    let trabajosValidos;
    try {
      validateDevices(dispositivosValidos);
      if (!descripcionProblema.trim()) throw new Error("Describe el problema reportado.");
      trabajosValidos = prepareWorks(trabajos);
    } catch (e) {
      setError(e.message);
      return;
    }

    if (clienteMode === "existente") {
      clienteRef = clientes.find((c) => c.id === clienteId);
      if (!clienteRef) {
        setError("Selecciona un cliente.");
        return;
      }
    } else {
      if (!nuevoNombre.trim() || nuevoNombre.trim().length > LIMITS.clientName) {
        setError("El nombre del cliente debe tener entre 1 y 100 caracteres.");
        return;
      }
    }
    setGuardando(true);
    try {
      if (!clienteRef) {
        clienteRef = await onCreateCliente({ nombre: nuevoNombre.trim(), telefono: joinPhone(nuevoTelefono) });
        // Si el ticket falla, el siguiente intento reutiliza este cliente.
        setClienteId(clienteRef.id);
        setClienteMode("existente");
      }
      await onCreateTicket({
      cliente: clienteRef,
      listaDispositivos: dispositivosValidos,
      listaTrabajos: trabajosValidos,
      descripcionProblema: descripcionProblema.trim(),
      comentariosCliente: comentariosCliente.trim(),
      });
    } catch (e) {
      setError(e?.message || "No se pudo crear el ticket.");
    } finally { setGuardando(false); }
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
              <label htmlFor="nuevo-cliente">Selecciona cliente</label>
              <Select
                id="nuevo-cliente"
                label="Selecciona cliente"
                menuLabel="Selecciona un cliente"
                value={clienteId ?? ""}
                onChange={setClienteId}
                options={clientes.map((c) => ({ value: c.id, label: `${c.nombre} · ${c.telefono}` }))}
              />
            </div>
          ) : (
            <div className="row2">
              <div className="field">
                <label>Nombre</label>
                <input
                  className="input"
                  placeholder="Nombre completo"
                  maxLength={LIMITS.clientName}
                  value={nuevoNombre}
                  onChange={(e) => setNuevoNombre(e.target.value)}
                />
              </div>
              <PhoneField value={nuevoTelefono} onChange={setNuevoTelefono} disabled={guardando} />
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
                aria-label="Modelo del dispositivo"
                maxLength={LIMITS.deviceName}
                value={d.modeloDispositivo}
                onChange={(e) => updateDispositivo(i, "modeloDispositivo", e.target.value)}
              />
              <input
                className="input"
                placeholder="Detalles / accesorios / daños visibles"
                aria-label="Detalles del dispositivo"
                maxLength={LIMITS.deviceDetails}
                value={d.detallesDispositivo}
                onChange={(e) => updateDispositivo(i, "detallesDispositivo", e.target.value)}
              />
              <Select
                label={`Plataforma del dispositivo ${i + 1}`}
                menuLabel="Selecciona una plataforma"
                value={d.plataforma}
                onChange={(value) => updateDispositivo(i, "plataforma", value)}
                options={Object.entries(PLATAFORMAS).map(([value, label]) => ({ value, label }))}
              />
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
                <Select
                  label={`Tipo de trabajo ${i + 1}`}
                  menuLabel="Selecciona un tipo de trabajo"
                  value={t.tipoTrabajo}
                  onChange={(value) => updateTrabajo(i, "tipoTrabajo", value)}
                  options={Object.entries(TIPOS_TRABAJO).map(([value, label]) => ({ value, label }))}
                />
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
                    aria-label="Nombre de la pieza"
                    maxLength={LIMITS.partName}
                    value={t.nombrePieza}
                    onChange={(e) => updateTrabajo(i, "nombrePieza", e.target.value)}
                  />
                  <input
                    className="input"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="Unidades"
                    aria-label="Unidades (máximo 50)"
                    max={LIMITS.units}
                    value={t.unidades}
                    onChange={(e) => updateTrabajo(i, "unidades", e.target.value)}
                  />
                  <input
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Precio por unidad"
                    aria-label="Precio por unidad (máximo 50,000 MXN)"
                    max={LIMITS.unitPrice}
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
        <button className="btn btn-primary" disabled={guardando} onClick={handleSubmit}>
          {guardando ? "Guardando…" : "Crear ticket"}
        </button>
      </div>
    </>
  );
}
