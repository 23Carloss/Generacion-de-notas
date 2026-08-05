/* =============================================================================
   CONFIGURACIÓN
   Cambia MOCK a false y ajusta API_BASE una vez que Capa_Negocio exponga
   endpoints REST (JAX-RS / servlets) sobre los DTOs existentes
   (ClienteDTO, DispositivoDTO, ResumenDTO, TrabajoDTO).

   Endpoints que este archivo asume del lado del backend:
     GET    /clientes
     POST   /clientes                body: ClienteDTO
     DELETE /clientes/{id}
     GET    /resumenes
     GET    /resumenes/{id}
     POST   /resumenes               body: ResumenDTO
     PUT    /resumenes/{id}/estado   body: { estado: string }
     DELETE /resumenes/{id}
   ============================================================================= */
export const CONFIG = {
  MOCK: false,
  API_BASE: "http://localhost:8080/GameMaintenance/api",
};

function delay(value) {
  return new Promise((resolve) => setTimeout(() => resolve(value), 120));
}

function clone(obj) {
  return JSON.parse(JSON.stringify(obj));
}

async function http(path, options) {
  const res = await fetch(CONFIG.API_BASE + path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) throw new Error("Error de red: " + res.status);
  if (res.status === 204) return null;
  return res.json();
}

/* ------------------------------- datos demo ------------------------------- */
let seq = { cliente: 100, dispositivo: 200, trabajo: 300, resumen: 400 };
const db = { clientes: [], resumenes: [] };

function seed() {
  const carlos = { id: ++seq.cliente, nombre: "Carlos Beltrán", telefono: "6444343343" };
  const ana = { id: ++seq.cliente, nombre: "Ana Duarte", telefono: "6441122334" };
  const luis = { id: ++seq.cliente, nombre: "Luis Peña", telefono: "6449988776" };
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
  });
  db.resumenes.push({
    id: ++seq.resumen,
    cliente: ana,
    estado: "Listo para entrega",
    fechaCreacion: new Date(Date.now() - 1000 * 60 * 60 * 70).toISOString(),
    descripcionProblema: "No lee discos.",
    comentariosCliente: "",
    listaDispositivos: [
      { id: ++seq.dispositivo, modeloDispositivo: "Xbox Series S", detallesDispositivo: "Sin caja, con cable HDMI", plataforma: "XBOX" },
    ],
    listaTrabajos: [{ id: ++seq.trabajo, tipoTrabajo: "REPARACION", precio: 480 }],
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
  });
}
seed();

/* --------------------------------- API ------------------------------------ */
export const Api = {
  clientes: {
    list: () => (CONFIG.MOCK ? delay(clone(db.clientes)) : http("/clientes")),
    create: (data) =>
      CONFIG.MOCK
        ? delay(
            clone(
              (() => {
                const c = { id: ++seq.cliente, ...data };
                db.clientes.push(c);
                return c;
              })()
            )
          )
        : http("/clientes", { method: "POST", body: JSON.stringify(data) }),
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
