import { useState } from "react";

export default function PublicarResena({ resumen, onPublicar }) {
  const [texto, setTexto] = useState(resumen.resenaPublica || "");
  const [confirmado, setConfirmado] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function guardar(retirar = false) {
    setBusy(true); setError("");
    try {
      await onPublicar(resumen.id, { texto: retirar ? null : texto.trim(), resenaOriginal: resumen.resenaComentario, confirmado });
    } catch (e) { setError(e.message || "No se pudo cambiar la publicación."); }
    finally { setBusy(false); }
  }

  if (resumen.estado !== "Entregado" || !resumen.resenaComentario || !resumen.calificacion) return null;
  return <div className="review-moderation">
    <h4>Reseña en trabajos resueltos</h4>
    <p className="hint">La reseña original es privada. Copia un fragmento literal que conserve su sentido, sin nombres, teléfonos, correos, direcciones, usuarios de redes u otros datos identificables. No se publica automáticamente.</p>
    {resumen.resenaPublica && <div className="resena-aviso"><strong>Fragmento publicado:</strong><p>{resumen.resenaPublica}</p></div>}
    <div className="field">
      <label htmlFor="review-excerpt">Fragmento anónimo para mostrar</label>
      <textarea id="review-excerpt" className="input" value={texto} maxLength={600} rows={4} disabled={busy}
        onChange={(e) => { setTexto(e.target.value); setConfirmado(false); }} />
      <span className="hint">{texto.length}/600 caracteres</span>
    </div>
    <label className="review-confirmation"><input type="checkbox" checked={confirmado} disabled={busy} onChange={(e) => setConfirmado(e.target.checked)} />
      He revisado que el fragmento no identifica a ninguna persona y conserva el sentido de la reseña.</label>
    {error && <p className="error-text" role="alert">{error}</p>}
    <div className="toolbar">
      <button className="btn btn-primary" disabled={busy || !confirmado || !texto.trim()} onClick={() => guardar()}>Publicar fragmento</button>
      {resumen.resenaPublica && <button className="btn btn-ghost" disabled={busy} onClick={() => guardar(true)}>Retirar fragmento</button>}
    </div>
    <p className="hint">Editar el ticket o su reseña, o reabrirlo, retira el fragmento para volver a revisarlo. Las estrellas siguen siendo anónimas.</p>
  </div>;
}
