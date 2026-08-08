import { useState } from "react";
 
export default function LoginView({ onLogin, onGoToRegister }) {
  const [correo, setCorreo] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [cargando, setCargando] = useState(false);
 
  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    if (!correo.trim() || !password) {
      setError("Correo y contraseña son obligatorios.");
      return;
    }
    setCargando(true);
    try {
      await onLogin(correo.trim(), password);
    } catch (err) {
      setError(err.message || "No se pudo iniciar sesión.");
    } finally {
      setCargando(false);
    }
  }
 
  return (
    <div className="auth-screen">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <p className="eyebrow">Game Maintenance</p>
        <h1 className="auth-title">Iniciar sesión</h1>
 
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
            placeholder="••••••••"
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
          {cargando ? "Entrando…" : "Entrar"}
        </button>
 
        <p className="auth-switch">
          ¿No tienes cuenta?{" "}
          <button type="button" className="link-button" onClick={onGoToRegister}>
            Regístrate
          </button>
        </p>
      </form>
    </div>
  );
}