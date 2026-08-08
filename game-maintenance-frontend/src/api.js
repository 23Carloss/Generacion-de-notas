/* =============================================================================
   CONFIGURACIÓN
   Cambia MOCK a false y ajusta API_BASE una vez que Capa_Negocio exponga
   endpoints REST (JAX-RS / servlets) sobre los DTOs existentes
   (ClienteDTO, DispositivoDTO, ResumenDTO, TrabajoDTO).

   Endpoints que este archivo asume del lado del backend:
     POST   /auth/login              body: {correo, password}                  -> {token, cliente}
     POST   /auth/register           body: {nombre, telefono, correo, password}-> {token, cliente}
     POST   /auth/logout             header Authorization: Bearer <token>
     GET    /clientes                (solo ADMINISTRADOR)
     POST   /clientes                body: ClienteDTO                (solo ADMINISTRADOR)
     PUT    /clientes/{id}           body: {nombre, telefono, correo}(solo el propio usuario)
     DELETE /clientes/{id}                                            (solo ADMINISTRADOR)
     GET    /resumenes               ADMINISTRADOR: todos · USUARIO: solo los suyos
     GET    /resumenes/{id}
     POST   /resumenes               body: ResumenDTO                (solo ADMINISTRADOR)
     PUT    /resumenes/{id}/estado   body: { estado: string }        (solo ADMINISTRADOR)
     PUT    /resumenes/{id}/resena   body: { calificacion, resenaComentario } (solo el dueño, ticket Entregado)
     DELETE /resumenes/{id}                                          (solo ADMINISTRADOR)

   Todas las peticiones (salvo /auth/*) mandan "Authorization: Bearer <token>"
   automáticamente si hay una sesión activa (ver getToken()/setToken() abajo).
   ============================================================================= */
export const CONFIG = {
  MOCK: false,
  API_BASE: "http://localhost:8080/GameMaintenance/api",
};

const TOKEN_KEY = "gm_token";

function delay(value) {
  return new Promise((resolve) => setTimeout(() => resolve(value), 120));
}

function clone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function http(path, options = {}) {
  const token = getToken();
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  if (token) headers.Authorization = "Bearer " + token;

  const res = await fetch(CONFIG.API_BASE + path, { ...options, headers });

  if (res.status === 401) {
    // Token inválido, ausente, o la sesión ya no existe en el servidor
    // (por ejemplo, se reinició). Se limpia la sesión local y se avisa a
    // App.jsx (vía evento) para que regrese a la pantalla de login.
    setToken(null);
    window.dispatchEvent(new Event("gm:unauthorized"));
    throw new ApiError("Sesión inválida, inicia sesión de nuevo.", 401);
  }

  if (!res.ok) {
    let mensaje = "Error de red: " + res.status;
    try {
      const body = await res.json();
      if (body && body.error) mensaje = body.error;
    } catch (_e) {
      // la respuesta no traía JSON; se deja el mensaje genérico
    }
    throw new ApiError(mensaje, res.status);
  }
  if (res.status === 204) return null;
  return res.json();
}

/* ------------------------------- datos demo (solo si CONFIG.MOCK = true) ------------------------------- */
let seq = { cliente: 100, dispositivo: 200, trabajo: 300, resumen: 400 };
const db = { clientes: [], resumenes: [] };

function seed() {
  const carlos = { id: ++seq.cliente, nombre: "Carlos Beltrán", telefono: "6444343343", rol: "USUARIO" };
  const ana = { id: ++seq.cliente, nombre: "Ana Duarte", telefono: "6441122334", rol: "USUARIO" };
  const luis = { id: ++seq.cliente, nombre: "Luis Peña", telefono: "6449988776", rol: "USUARIO" };
  db.clientes.push(carlos, ana, luis);

  db.resumenes.push({
    id: ++seq.resumen,
    cliente: carlos,
    estado: "En reparación",
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(),
    descripcionProblema: "Sobrecalentamiento: se apaga sola tras 40 min de uso.",
    comentariosCliente: "A veces enciende, a veces no. Urge para el fin de semana.",
    listaDispositivos: [
      { id: ++seq.dispositivo, modeloDispositivo: "PS4 Slim", detallesDispositivo: "Color negro, 2 controles, rayón lado derecho", plataforma: "PLAYSTATION" },
    ],
    listaTrabajos: [
      { id: ++seq.trabajo, tipoTrabajo: "DIAGNOSTICO", precio: 150 },
      { id: ++seq.trabajo, tipoTrabajo: "MANTENIMIENTO", precio: 350 },
    ],
    resenaComentario: null,
    calificacion: null,
  });
  db.resumenes.push({
    id: ++seq.resumen,
    cliente: ana,
    estado: "Entregado",
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 60 * 70).toISOString(),
    descripcionProblema: "No lee discos.",
    comentariosCliente: "",
    listaDispositivos: [
      { id: ++seq.dispositivo, modeloDispositivo: "Xbox Series S", detallesDispositivo: "Sin caja, con cable HDMI", plataforma: "XBOX" },
    ],
    listaTrabajos: [{ id: ++seq.trabajo, tipoTrabajo: "REPARACION", precio: 480 }],
    resenaComentario: "Muy buen servicio, rápido y a buen precio.",
    calificacion: 5,
  });
  db.resumenes.push({
    id: ++seq.resumen,
    cliente: luis,
    estado: "Recibido",
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 30).toISOString(),
    descripcionProblema: "Laptop no enciende desde ayer.",
    comentariosCliente: "Se mojó ligeramente con lluvia.",
    listaDispositivos: [
      { id: ++seq.dispositivo, modeloDispositivo: "Laptop Acer Nitro 5", detallesDispositivo: "Golpe leve en bisagra", plataforma: "LAPTOP" },
    ],
    listaTrabajos: [{ id: ++seq.trabajo, tipoTrabajo: "DIAGNOSTICO", precio: 200 }],
    resenaComentario: null,
    calificacion: null,
  });
  db.resumenes.push({
    id: ++seq.resumen,
    cliente: carlos,
    estado: "Entregado",
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 60 * 24 * 6).toISOString(),
    descripcionProblema: "Chipeo para lectura de respaldos.",
    comentariosCliente: "",
    listaDispositivos: [
      { id: ++seq.dispositivo, modeloDispositivo: "Xbox 360", detallesDispositivo: "Modelo Slim", plataforma: "XBOX" },
    ],
    listaTrabajos: [{ id: ++seq.trabajo, tipoTrabajo: "CHIPEO", precio: 600 }],
    resenaComentario: null,
    calificacion: null,
  });
}
seed();

const MOCK_ADMIN = { id: 1, nombre: "Admin Demo", telefono: "6440000000", correo: "admin@demo.com", rol: "ADMINISTRADOR" };

/* --------------------------------- API ------------------------------------ */
export const Api = {
  auth: {
    login: (correo, password) =>
      CONFIG.MOCK
        ? delay({ token: "mock-token", cliente: MOCK_ADMIN }).then((r) => {
            setToken(r.token);
            return r.cliente;
          })
        : http("/auth/login", { method: "POST", body: JSON.stringify({ correo, password }) }).then((r) => {
            setToken(r.token);
            return r.cliente;
          }),
    register: (data) =>
      CONFIG.MOCK
        ? delay({
            token: "mock-token",
            cliente: { id: ++seq.cliente, ...data, rol: "USUARIO" },
          }).then((r) => {
            setToken(r.token);
            return r.cliente;
          })
        : http("/auth/register", { method: "POST", body: JSON.stringify(data) }).then((r) => {
            setToken(r.token);
            return r.cliente;
          }),
    logout: () =>
      CONFIG.MOCK
        ? delay(true).then(() => setToken(null))
        : http("/auth/logout", { method: "POST" }).finally(() => setToken(null)),
  },
  clientes: {
    list: () => (CONFIG.MOCK ? delay(clone(db.clientes)) : http("/clientes")),
    create: (data) =>
      CONFIG.MOCK
        ? delay(
            clone(
              (() => {
                const c = { id: ++seq.cliente, rol: "USUARIO", ...data };
                db.clientes.push(c);
                return c;
              })()
            )
          )
        : http("/clientes", { method: "POST", body: JSON.stringify(data) }),
    update: (id, data) =>
      CONFIG.MOCK
        ? delay(
            (() => {
              const c = db.clientes.find((x) => x.id === id);
              if (c) Object.assign(c, data);
              return clone(c);
            })()
          )
        : http("/clientes/" + id, { method: "PUT", body: JSON.stringify(data) }),
    remove: (id) =>
      CONFIG.MOCK
        ? delay(
            (() => {
              db.clientes = db.clientes.filter((c) => c.id !== id);
              return true;
            })()
          )
        : http("/clientes/" + id, { method: "DELETE" }),
  },
  resumenes: {
    list: () => (CONFIG.MOCK ? delay(clone(db.resumenes)) : http("/resumenes")),
    get: (id) =>
      CONFIG.MOCK
        ? delay(clone(db.resumenes.find((r) => r.id === id)))
        : http("/resumenes/" + id),
    create: (data) =>
      CONFIG.MOCK
        ? delay(
            clone(
              (() => {
                const r = {
                  id: ++seq.resumen,
                  fechaCreacion: new Date().toISOString(),
                  estado: "Recibido",
                  resenaComentario: null,
                  calificacion: null,
                  ...data,
                };
                r.listaDispositivos = (data.listaDispositivos || []).map((d) => ({
                  id: ++seq.dispositivo,
                  ...d,
                }));
                r.listaTrabajos = (data.listaTrabajos || []).map((t) => ({
                  id: ++seq.trabajo,
                  ...t,
                }));
                db.resumenes.unshift(r);
                return r;
              })()
            )
          )
        : http("/resumenes", { method: "POST", body: JSON.stringify(data) }),
    updateEstado: (id, estado) =>
      CONFIG.MOCK
        ? delay(
            (() => {
              const r = db.resumenes.find((x) => x.id === id);
              if (r) r.estado = estado;
              return r;
            })()
          )
        : http("/resumenes/" + id + "/estado", {
            method: "PUT",
            body: JSON.stringify({ estado }),
          }),
    updateResena: (id, data) =>
      CONFIG.MOCK
        ? delay(
            (() => {
              const r = db.resumenes.find((x) => x.id === id);
              if (r) Object.assign(r, data);
              return clone(r);
            })()
          )
        : http("/resumenes/" + id + "/resena", { method: "PUT", body: JSON.stringify(data) }),
    remove: (id) =>
      CONFIG.MOCK
        ? delay(
            (() => {
              db.resumenes = db.resumenes.filter((r) => r.id !== id);
              return true;
            })()
          )
        : http("/resumenes/" + id, { method: "DELETE" }),
  },
};