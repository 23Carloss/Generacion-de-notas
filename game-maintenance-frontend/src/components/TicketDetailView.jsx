import { useEffect, useState } from "react";
import Badge from "./Badge";
import EmptyState from "./EmptyState";
import Stars from "./Stars";
import Select from "./Select";
import { ESTADOS, PLATAFORMAS, TIPOS_TRABAJO } from "../constants";
import { money, fecha, totalTicket } from "../utils";
import { LIMITS, prepareWorks, validateDevices } from "../formValidation";

export default function TicketDetailView({
  resumen,
  esAdmin,
  onBack,
  onUpdateEstado,
  onUpdateTicket,
  onUpdateResena,
  onUploadImagen,
  onRemoveImagen,
  onDelete,
  clientes,
}) {
  const [confirming, setConfirming] = useState(false);
  const [editing, setEditing] = useState(false);

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

  if (editing) {
    return (
      <>
        <button className="back-link" onClick={() => setEditing(false)}>
          ← Cancelar edición
        </button>
        <div className="view-header">
          <div>
            <p className="eyebrow">Ticket #{resumen.id}</p>
            <h1>Editar ticket</h1>
            <p>Actualiza el cliente, el equipo, el problema o los trabajos registrados.</p>
          </div>
        </div>
        <div className="card">
          <TicketEditor
            resumen={resumen}
            clientes={clientes}
            onCancel={() => setEditing(false)}
            onSave={async (data) => {
              await onUpdateTicket(resumen.id, data);
              setEditing(false);
            }}
          />
        </div>
      </>
    );
  }

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
                      <td>
                        {TIPOS_TRABAJO[t.tipoTrabajo] || t.tipoTrabajo}
                        {t.tipoTrabajo === "REPARACION" && t.nombrePieza && (
                          <div className="hint">
                            {t.nombrePieza} · {t.unidades} {t.unidades === 1 ? "unidad" : "unidades"} × {money(t.precioUnitario)}
                          </div>
                        )}
                      </td>
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

          <div className="subblock">
            <h3>Fotos</h3>
            <GaleriaFotos
              imagenes={resumen.listaImagenes}
              esAdmin={esAdmin}
              onRemoveImagen={(imagenId) => onRemoveImagen(resumen.id, imagenId)}
            />
            {esAdmin && <FormularioSubirFoto resumenId={resumen.id} onUploadImagen={onUploadImagen} />}
          </div>

          <div className="subblock">
            <h3>Reseña del cliente</h3>
            {esAdmin ? (
              <ReseñaSoloLectura resumen={resumen} />
            ) : (
              <ReseñaFormulario resumen={resumen} onUpdateResena={onUpdateResena} />
            )}
          </div>
        </div>

        <div className="card">
          {esAdmin && (
            <div className="field">
              <label htmlFor="ticket-estado">Actualizar estado</label>
              <Select
                id="ticket-estado"
                label="Actualizar estado"
                menuLabel="Selecciona un estado"
                value={resumen.estado}
                onChange={(value) => onUpdateEstado(resumen.id, value)}
                options={ESTADOS.map((value) => ({ value, label: value }))}
              />
            </div>
          )}
          <div className="field">
            <label>Teléfono de contacto</label>
            <div>{resumen.cliente ? resumen.cliente.telefono : "—"}</div>
          </div>

          {esAdmin && (
            <>
              <button
                className="btn btn-primary btn-small"
                style={{ width: "100%", justifyContent: "center", marginBottom: ".7rem" }}
                onClick={() => setEditing(true)}
              >
                Editar ticket
              </button>
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
            </>
          )}
        </div>
      </div>
    </>
  );
}

function blankDispositivo() {
  return { modeloDispositivo: "", detallesDispositivo: "", plataforma: "PLAYSTATION" };
}

function blankTrabajo() {
  return { tipoTrabajo: "DIAGNOSTICO", precio: "", nombrePieza: "", unidades: "", precioUnitario: "" };
}

function TicketEditor({ resumen, clientes, onCancel, onSave }) {
  const [clienteId, setClienteId] = useState(resumen.cliente?.id || "");
  const [descripcionProblema, setDescripcionProblema] = useState(resumen.descripcionProblema || "");
  const [comentariosCliente, setComentariosCliente] = useState(resumen.comentariosCliente || "");
  const [dispositivos, setDispositivos] = useState(
    (resumen.listaDispositivos || []).map((d) => ({ ...d }))
  );
  const [trabajos, setTrabajos] = useState(
    (resumen.listaTrabajos || []).map((t) => ({
      ...t,
      precio: t.precio ?? "",
      nombrePieza: t.nombrePieza || "",
      unidades: t.unidades ?? "",
      precioUnitario: t.precioUnitario ?? "",
    }))
  );
  const [error, setError] = useState("");
  const [guardando, setGuardando] = useState(false);

  useEffect(() => {
    setClienteId(resumen.cliente?.id || "");
    setDescripcionProblema(resumen.descripcionProblema || "");
    setComentariosCliente(resumen.comentariosCliente || "");
    setDispositivos((resumen.listaDispositivos || []).map((d) => ({ ...d })));
    setTrabajos(
      (resumen.listaTrabajos || []).map((t) => ({
        ...t,
        precio: t.precio ?? "",
        nombrePieza: t.nombrePieza || "",
        unidades: t.unidades ?? "",
        precioUnitario: t.precioUnitario ?? "",
      }))
    );
  }, [resumen]);

  function updateDispositivo(index, field, value) {
    setDispositivos((prev) => prev.map((d, i) => (i === index ? { ...d, [field]: value } : d)));
  }

  function updateTrabajo(index, field, value) {
    setTrabajos((prev) =>
      prev.map((t, i) => {
        if (i !== index) return t;
        if (field === "tipoTrabajo" && value !== "REPARACION") {
          return { ...t, tipoTrabajo: value, nombrePieza: "", unidades: "", precioUnitario: "" };
        }
        return { ...t, [field]: value };
      })
    );
  }

  async function guardar() {
    setError("");
    if (!clienteId) {
      setError("Selecciona un cliente.");
      return;
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

    let trabajosValidos;
    try {
      validateDevices(dispositivosValidos);
      trabajosValidos = prepareWorks(trabajos);
    } catch (e) {
      setError(e.message);
      return;
    }

    setGuardando(true);
    try {
      await onSave({
        cliente: { id: Number(clienteId) },
        listaDispositivos: dispositivosValidos.map(({ modeloDispositivo, detallesDispositivo, plataforma }) => ({
          modeloDispositivo: modeloDispositivo.trim(),
          detallesDispositivo: (detallesDispositivo || "").trim(),
          plataforma,
        })),
        listaTrabajos: trabajosValidos,
        descripcionProblema: descripcionProblema.trim(),
        comentariosCliente: comentariosCliente.trim(),
      });
    } catch (e) {
      setError(e?.message || "No se pudo guardar el ticket.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <div className="subblock">
        <h3>Cliente</h3>
        <div className="field">
          <label htmlFor="editar-cliente">Cliente asignado</label>
          <Select
            id="editar-cliente"
            label="Cliente asignado"
            menuLabel="Selecciona un cliente"
            value={clienteId}
            onChange={setClienteId}
            options={[
              { value: "", label: "Selecciona un cliente" },
              ...clientes.map((cliente) => ({ value: cliente.id, label: `${cliente.nombre} · ${cliente.telefono}` })),
            ]}
          />
        </div>
      </div>

      <div className="subblock">
        <h3>Dispositivos</h3>
        {dispositivos.map((dispositivo, index) => (
          <div className="repeat-row" key={index}>
            <input className="input" placeholder="Modelo" maxLength={LIMITS.deviceName} value={dispositivo.modeloDispositivo}
              onChange={(e) => updateDispositivo(index, "modeloDispositivo", e.target.value)} />
            <input className="input" placeholder="Detalles / accesorios" maxLength={LIMITS.deviceDetails} value={dispositivo.detallesDispositivo || ""}
              onChange={(e) => updateDispositivo(index, "detallesDispositivo", e.target.value)} />
            <Select
              label={`Plataforma del dispositivo ${index + 1}`}
              menuLabel="Selecciona una plataforma"
              value={dispositivo.plataforma}
              onChange={(value) => updateDispositivo(index, "plataforma", value)}
              options={Object.entries(PLATAFORMAS).map(([value, label]) => ({ value, label }))}
            />
            <button className="remove-x" type="button" title="Quitar dispositivo"
              onClick={() => setDispositivos((prev) => prev.filter((_, i) => i !== index))}>✕</button>
          </div>
        ))}
        <button className="btn btn-ghost btn-small" type="button" onClick={() => setDispositivos((prev) => [...prev, blankDispositivo()])}>
          + Agregar dispositivo
        </button>
      </div>

      <div className="row2">
        <div className="field">
          <label>Descripción del problema</label>
          <textarea className="input" rows={3} value={descripcionProblema} onChange={(e) => setDescripcionProblema(e.target.value)} />
        </div>
        <div className="field">
          <label>Comentarios del cliente</label>
          <textarea className="input" rows={3} value={comentariosCliente} onChange={(e) => setComentariosCliente(e.target.value)} />
        </div>
      </div>

      <div className="subblock">
        <h3>Trabajos</h3>
        {trabajos.map((trabajo, index) => (
          <div className="trabajo-editor" key={index}>
            <div className="repeat-row trabajo-row">
              <Select
                label={`Tipo de trabajo ${index + 1}`}
                menuLabel="Selecciona un tipo de trabajo"
                value={trabajo.tipoTrabajo}
                onChange={(value) => updateTrabajo(index, "tipoTrabajo", value)}
                options={Object.entries(TIPOS_TRABAJO).map(([value, label]) => ({ value, label }))}
              />
              {trabajo.tipoTrabajo === "REPARACION" ? (
                <div className="repair-price-note">El total se calcula con las unidades y el precio por unidad.</div>
              ) : (
                <input className="input" type="number" min="0" step="0.01" placeholder="Precio" value={trabajo.precio}
                  onChange={(e) => updateTrabajo(index, "precio", e.target.value)} />
              )}
              <button className="remove-x" type="button" title="Quitar trabajo"
                onClick={() => setTrabajos((prev) => prev.filter((_, i) => i !== index))}>✕</button>
            </div>
            {trabajo.tipoTrabajo === "REPARACION" && (
              <div className="repair-fields">
                <input className="input" placeholder="Nombre de la pieza" maxLength={LIMITS.partName} value={trabajo.nombrePieza}
                  onChange={(e) => updateTrabajo(index, "nombrePieza", e.target.value)} />
                <input className="input" type="number" min="1" max={LIMITS.units} step="1" placeholder="Unidades" value={trabajo.unidades}
                  onChange={(e) => updateTrabajo(index, "unidades", e.target.value)} />
                <input className="input" type="number" min="0" max={LIMITS.unitPrice} step="0.01" placeholder="Precio por unidad" value={trabajo.precioUnitario}
                  onChange={(e) => updateTrabajo(index, "precioUnitario", e.target.value)} />
              </div>
            )}
          </div>
        ))}
        <button className="btn btn-ghost btn-small" type="button" onClick={() => setTrabajos((prev) => [...prev, blankTrabajo()])}>
          + Agregar trabajo
        </button>
      </div>

      {error && <div className="error-text">{error}</div>}
      <div className="toolbar" style={{ marginBottom: 0 }}>
        <button className="btn btn-primary" type="button" disabled={guardando} onClick={guardar}>
          {guardando ? "Guardando…" : "Guardar cambios"}
        </button>
        <button className="btn btn-ghost" type="button" disabled={guardando} onClick={onCancel}>Cancelar</button>
      </div>
    </>
  );
}

/* --------------------------- Galería de fotos --------------------------- */
// Visible para ambos roles (usuario y administrador). Cada foto se muestra
// junto a su fecha de subida y descripción, a modo de pequeño "log" de la
// subida del archivo.
function GaleriaFotos({ imagenes, esAdmin, onRemoveImagen }) {
  const [eliminando, setEliminando] = useState(null);
  const [error, setError] = useState("");

  async function eliminarFoto(imagenId) {
    setError("");
    setEliminando(imagenId);
    try {
      await onRemoveImagen(imagenId);
    } catch (e) {
      setError(e?.message || "No se pudo eliminar la foto.");
    } finally {
      setEliminando(null);
    }
  }

  if (!imagenes || !imagenes.length) {
    return <p className="hint">Sin fotos todavía.</p>;
  }
  return (
    <>
      <div className="photo-grid">
        {imagenes.map((img) => (
          <figure className="photo-item" key={img.id}>
            <img src={img.dataBase64} alt={img.descripcion || "Foto del ticket"} />
            {esAdmin && (
              <button
                className="photo-remove"
                type="button"
                title="Eliminar foto"
                aria-label="Eliminar foto"
                disabled={eliminando === img.id}
                onClick={() => eliminarFoto(img.id)}
              >
                {eliminando === img.id ? "…" : "×"}
              </button>
            )}
            <figcaption>
              {img.tipo && <span className="photo-tipo">{img.tipo === "ANTES" ? "Antes" : "Después"}</span>}
              <span className="hint">
                {fecha(img.fechaSubida)}
                {img.descripcion ? " · " + img.descripcion : ""}
              </span>
            </figcaption>
          </figure>
        ))}
      </div>
      {error && <div className="error-text">{error}</div>}
    </>
  );
}

/* -------------------------- Subir foto (admin) -------------------------- */
const TAMANO_MAX_FOTO = 1024 * 1024;
const TIPOS_IMAGEN_PERMITIDOS = ["image/png", "image/jpeg", "image/webp"];

function FormularioSubirFoto({ resumenId, onUploadImagen }) {
  const [dataBase64, setDataBase64] = useState(null);
  const [nombreArchivo, setNombreArchivo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [tipo, setTipo] = useState("");
  const [error, setError] = useState("");
  const [subiendo, setSubiendo] = useState(false);
  const [inputKey, setInputKey] = useState(0);

  function limpiarArchivo() {
    setDataBase64(null);
    setNombreArchivo("");
    setInputKey((key) => key + 1);
  }

  function handleFile(e) {
    setError("");
    const file = e.target.files && e.target.files[0];
    if (!file) {
      setDataBase64(null);
      setNombreArchivo("");
      return;
    }
    if (!TIPOS_IMAGEN_PERMITIDOS.includes(file.type)) {
      setError("Solo se permiten imágenes PNG, JPEG o WebP.");
      e.target.value = "";
      limpiarArchivo();
      return;
    }
    if (file.size > TAMANO_MAX_FOTO) {
      setError("La imagen pesa demasiado (máximo 1 MB).");
      e.target.value = "";
      limpiarArchivo();
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      // reader.result ya viene como Data URL completa ("data:image/...;base64,...")
      setDataBase64(reader.result);
      setNombreArchivo(file.name);
    };
    reader.onerror = () => setError("No se pudo leer el archivo.");
    reader.readAsDataURL(file);
  }

  async function handleSubmit() {
    setError("");
    if (!dataBase64) {
      setError("Selecciona una imagen.");
      return;
    }
    setSubiendo(true);
    try {
      await onUploadImagen(resumenId, { dataBase64, descripcion: descripcion.trim(), tipo: tipo || null });
      limpiarArchivo();
      setDescripcion("");
      setTipo("");
    } catch (e) {
      setError(e?.message || "No se pudo subir la foto.");
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <div className="upload-photo-form">
      <div className="field">
        <label>Subir foto desde mi dispositivo</label>
        <label className="file-picker">
          <input
            key={inputKey}
            type="file"
            accept="image/png,image/jpeg,image/webp"
            onChange={handleFile}
          />
          <span className="file-picker-icon">↑</span>
          <span>{nombreArchivo ? "Cambiar archivo" : "Seleccionar imagen"}</span>
          <small>PNG, JPG o WebP · Máximo 1 MB</small>
        </label>
        {nombreArchivo && (
          <div className="selected-file">
            <span>✓ {nombreArchivo}</span>
            <button type="button" className="remove-x" title="Quitar archivo" onClick={limpiarArchivo}>
              ✕
            </button>
          </div>
        )}
      </div>
      <div className="row2">
        <div className="field">
          <label>Descripción (opcional)</label>
          <input
            className="input"
            placeholder="Ej. Rayón en la carcasa"
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
          />
        </div>
        <div className="field">
          <label htmlFor="foto-tipo">Tipo (opcional)</label>
          <Select
            id="foto-tipo"
            label="Tipo de foto (opcional)"
            menuLabel="Selecciona un tipo de foto"
            value={tipo}
            onChange={setTipo}
            options={[
              { value: "", label: "Sin especificar" },
              { value: "ANTES", label: "Antes" },
              { value: "DESPUES", label: "Después" },
            ]}
          />
        </div>
      </div>
      {error && <div className="error-text">{error}</div>}
      <button className="btn btn-ghost btn-small" onClick={handleSubmit} disabled={subiendo}>
        {subiendo ? "Subiendo…" : "Subir foto"}
      </button>
    </div>
  );
}

/* ------------------- Reseña: solo-lectura (administrador) ------------------- */
function ReseñaSoloLectura({ resumen }) {
  if (!resumen.calificacion) {
    return <p className="hint">El cliente todavía no ha dejado una reseña.</p>;
  }
  return (
    <div>
      <Stars value={resumen.calificacion} />
      {resumen.resenaComentario && (
        <p className="hint" style={{ marginTop: ".5rem" }}>
          {resumen.resenaComentario}
        </p>
      )}
    </div>
  );
}

/* --------------------- Reseña: formulario editable (usuario) --------------------- */
// Regla de negocio (ya implementada en el backend, ResumenService.actualizarResena):
// solo el cliente dueño del ticket puede publicarla/editarla, y solo cuando
// el ticket ya está en estado "Entregado". Mientras no lo esté, el
// formulario se muestra deshabilitado con un aviso.
function ReseñaFormulario({ resumen, onUpdateResena }) {
  const [calificacion, setCalificacion] = useState(resumen.calificacion || 0);
  const [comentario, setComentario] = useState(resumen.resenaComentario || "");
  const [error, setError] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [enviado, setEnviado] = useState(false);

  useEffect(() => {
    setCalificacion(resumen.calificacion || 0);
    setComentario(resumen.resenaComentario || "");
    setEnviado(false);
    // Solo se debe re-sincronizar cuando cambia de ticket (navegación),
    // no en cada actualización de resumen mientras el usuario está
    // escribiendo su reseña (perdería lo que lleva escrito).
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resumen.id]);

  const habilitado = resumen.estado === "Entregado";

  async function handlePublicar() {
    setError("");
    if (!calificacion) {
      setError("Selecciona una calificación de 1 a 5 estrellas.");
      return;
    }
    setEnviando(true);
    try {
      await onUpdateResena(resumen.id, { calificacion, resenaComentario: comentario.trim() });
      setEnviado(true);
    } catch (e) {
      setError(e?.message || "No se pudo publicar la reseña.");
    } finally {
      setEnviando(false);
    }
  }

  if (!habilitado) {
    return (
      <div className="resena-aviso">
        Podrás calificar el servicio y dejar tu comentario cuando el ticket esté marcado como
        Entregado.
      </div>
    );
  }

  return (
    <div>
      <div className="field">
        <label>Tu calificación</label>
        <Stars value={calificacion} onChange={setCalificacion} />
      </div>
      <div className="field">
        <label>Tu comentario</label>
        <textarea
          className="input"
          rows={3}
          placeholder="Cuéntanos cómo fue tu experiencia con el servicio…"
          value={comentario}
          onChange={(e) => setComentario(e.target.value)}
        />
      </div>
      {error && <div className="error-text">{error}</div>}
      <button className="btn btn-primary btn-small" onClick={handlePublicar} disabled={enviando}>
        {enviando ? "Publicando…" : resumen.calificacion ? "Actualizar reseña" : "Publicar reseña"}
      </button>
      {enviado && <p className="hint" style={{ marginTop: ".5rem" }}>¡Gracias por tu reseña!</p>}
    </div>
  );
}
