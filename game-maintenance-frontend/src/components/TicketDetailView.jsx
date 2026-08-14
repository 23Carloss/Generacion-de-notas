import { useEffect, useState } from "react";
import Badge from "./Badge";
import EmptyState from "./EmptyState";
import Stars from "./Stars";
import { ESTADOS, PLATAFORMAS, TIPOS_TRABAJO } from "../constants";
import { money, fecha, totalTicket } from "../utils";

export default function TicketDetailView({
  resumen,
  esAdmin,
  onBack,
  onUpdateEstado,
  onUpdateResena,
  onUploadImagen,
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

          <div className="subblock">
            <h3>Fotos</h3>
            <GaleriaFotos imagenes={resumen.listaImagenes} />
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
          )}
          <div className="field">
            <label>Teléfono de contacto</label>
            <div>{resumen.cliente ? resumen.cliente.telefono : "—"}</div>
          </div>

          {esAdmin && (
            <>
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

/* --------------------------- Galería de fotos --------------------------- */
// Visible para ambos roles (usuario y administrador). Cada foto se muestra
// junto a su fecha de subida y descripción, a modo de pequeño "log" de la
// subida del archivo.
function GaleriaFotos({ imagenes }) {
  if (!imagenes || !imagenes.length) {
    return <p className="hint">Sin fotos todavía.</p>;
  }
  return (
    <div className="photo-grid">
      {imagenes.map((img) => (
        <figure className="photo-item" key={img.id}>
          <img src={img.dataBase64} alt={img.descripcion || "Foto del ticket"} />
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
  );
}

/* -------------------------- Subir foto (admin) -------------------------- */
const TAMANO_MAX_FOTO = 5 * 1024 * 1024; // 5 MB, para no mandar payloads gigantes

function FormularioSubirFoto({ resumenId, onUploadImagen }) {
  const [dataBase64, setDataBase64] = useState(null);
  const [nombreArchivo, setNombreArchivo] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [tipo, setTipo] = useState("");
  const [error, setError] = useState("");
  const [subiendo, setSubiendo] = useState(false);

  function handleFile(e) {
    setError("");
    const file = e.target.files && e.target.files[0];
    if (!file) {
      setDataBase64(null);
      setNombreArchivo("");
      return;
    }
    if (file.size > TAMANO_MAX_FOTO) {
      setError("La imagen pesa demasiado (máximo 5 MB).");
      e.target.value = "";
      setDataBase64(null);
      setNombreArchivo("");
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
      setDataBase64(null);
      setNombreArchivo("");
      setDescripcion("");
      setTipo("");
    } catch (e) {
      setError(e?.message || "No se pudo subir la foto.");
    } finally {
      setSubiendo(false);
    }
  }

  return (
    <div style={{ marginTop: ".8rem", paddingTop: ".8rem", borderTop: "1px dashed var(--panel-border)" }}>
      <div className="field">
        <label>Subir foto desde mi dispositivo</label>
        <input className="input" type="file" accept="image/*" onChange={handleFile} />
        {nombreArchivo && <p className="hint" style={{ margin: ".3rem 0 0" }}>Seleccionada: {nombreArchivo}</p>}
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
          <label>Tipo (opcional)</label>
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="">Sin especificar</option>
            <option value="ANTES">Antes</option>
            <option value="DESPUES">Después</option>
          </select>
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


