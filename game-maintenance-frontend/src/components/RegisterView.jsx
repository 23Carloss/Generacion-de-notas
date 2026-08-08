import { useState } from "react";

export default function RegisterView({ onRegister, onGoToLogin }) {
  const [nombre, setNombre] = useState("");
  const [telefono, setTelefono] = useState("");
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    if (!nombre.trim() || !telefono.trim() || !correo.trim() || !password) {
      setError("Todos los campos son obligatorios.");
      return;
    }
    if (password.length < 8) {
      setError("La contraseña debe tener al menos 8 caracteres.");
      return;
    }
    setCargando(true);
    try {
      await onRegister({
        nombre: nombre.trim(),
        telefono: telefono.trim(),
        correo: correo.trim(),
        password,
      });
    } catch (err) {
      setError(err.message || "No se pudo crear la cuenta.");
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="auth-screen">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Game Maintenance</p>
        <h1 className="auth-title">Crear cuenta</h1>

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
        <div className="field">
          <label>Correo</label>
          <input
            className="input"
            type="email"
            placeholder="tu@correo.com"
            value={correo}
            onChange={(e) => setCorreo(e.target.value)}
          />
        </div>
        <div className="field">
          <label>Contraseña</label>
          <input
            className="input"
            type="password"
            placeholder="Mínimo 8 caracteres"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        {error && <div className="error-text">{error}</div>}

        <button
          className="btn btn-primary"
          type="submit"
          disabled={cargando}
          style={{ width: "100%", justifyContent: "center" }}
        >
          {cargando ? "Creando cuenta…" : "Crear cuenta"}
        </button>

        <p className="auth-switch">
          ¿Ya tienes cuenta?{" "}
          <button type="button" className="link-button" onClick={onGoToLogin}>
            Inicia sesión
          </button>
        </p>
      </form>
    </div>
  );
}