import { useState } from "react";
import { ROLES } from "../constants";

export default function PerfilView({ usuario, onUpdate }) {
  const [nombre, setNombre] = useState(usuario.nombre || "");
  const [telefono, setTelefono] = useState(usuario.telefono || "");
  const [correo, setCorreo] = useState(usuario.correo || "");
  const [error, setError] = useState("");
  const [mensaje, setMensaje] = useState("");
  const [guardando, setGuardando] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setMensaje("");
    if (!nombre.trim() || !telefono.trim() || !correo.trim()) {
      setError("Nombre, teléfono y correo son obligatorios.");
      return;
    }
    setGuardando(true);
    try {
      await onUpdate({ nombre: nombre.trim(), telefono: telefono.trim(), correo: correo.trim() });
      setMensaje("Perfil actualizado.");
    } catch (err) {
      setError(err.message || "No se pudo actualizar el perfil.");
    } finally {
      setGuardando(false);
    }
  }

  return (
    <>
      <div className="view-header">
        <div>
          <p className="eyebrow">Cuenta</p>
          <h1>Mi perfil</h1>
          <p>Actualiza tu información de contacto.</p>
        </div>
      </div>

      <form className="card" style={{ maxWidth: 480 }} onSubmit={handleSubmit}>
        <div className="field">
          <label>Rol</label>
          <div>{ROLES[usuario.rol] || usuario.rol}</div>
        </div>
        <div className="field">
          <label>Nombre</label>
          <input className="input" value={nombre} onChange={(e) => setNombre(e.target.value)} />
        </div>
        <div className="field">
          <label>Teléfono</label>
          <input className="input" value={telefono} onChange={(e) => setTelefono(e.target.value)} />
        </div>
        <div className="field">
          <label>Correo</label>
          <input
            className="input"
            type="email"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />
        </div>

        {error && <div className="error-text">{error}</div>}
        {mensaje && (
          <div className="hint" style={{ color: "var(--teal)", marginBottom: ".8rem" }}>
            {mensaje}
          </div>
        )}

        <button className="btn btn-primary" type="submit" disabled={guardando}>
          {guardando ? "Guardando…" : "Guardar cambios"}
        </button>
      </form>
    </>
  );
}