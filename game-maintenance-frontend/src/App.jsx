import { useEffect, useState, useCallback } from "react";
import Sidebar from "./components/Sidebar";
import DashboardView from "./components/DashboardView";
import TicketsView from "./components/TicketsView";
import TicketDetailView from "./components/TicketDetailView";
import ClientesView from "./components/ClientesView";
import NuevoTicketView from "./components/NuevoTicketView";
import LoginView from "./components/Loginview";
import RegisterView from "./components/RegisterView";
import PerfilView from "./components/PerfilView";
import TrabajosResueltosView from "./components/TrabajosResueltosView";
import NotificacionesView from "./components/NotificacionesView";
import { Api, getToken } from "./api";

function parseHash() {
  const h = window.location.hash.replace(/^#\/?/, "");
  if (h.startsWith("ticket/")) {
    const [, id, section] = h.split("/");
    return { route: "ticket", ticketId: Number(id), clientId: null, section: section || null };
  }
  if (h.startsWith("cliente/")) {
    return { route: "clientes", ticketId: null, clientId: Number(h.split("/")[1]), section: null };
  }
  if (["dashboard", "nuevo", "tickets", "clientes", "perfil", "login", "registro", "resueltos", "notificaciones"].includes(h)) {
    return { route: h, ticketId: null, clientId: null, section: null };
  }
  return { route: "dashboard", ticketId: null, clientId: null, section: null };
}

export default function App() {
  const [{ route, ticketId, clientId, section }, setRouteState] = useState(parseHash());
  const [usuario, setUsuario] = useState(null);
  const [clientes, setClientes] = useState([]);
  const [resumenes, setResumenes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadErrors, setLoadErrors] = useState({});
  const [unreadCount, setUnreadCount] = useState(0);

  // sesionActiva revisa tanto el usuario guardado como que exista un token:
  // si el token se limpió (por un 401) pero el usuario seguía en el estado
  // de React, no queremos tratarlo como "logueado".
  const sesionActiva = !!usuario && !!getToken();
  const esAdmin = usuario?.rol === "ADMINISTRADOR";

  const refresh = useCallback(async () => {
    if (!sesionActiva) return;
    setLoading(true);
    setLoadErrors({});
    try {
      const resumenesPromise = Api.resumenes.list();
      const clientesPromise = esAdmin ? Api.clientes.list() : Promise.resolve([]);
      const notificacionesPromise = Api.notificaciones.unreadCount();
      const [r, c, n] = await Promise.allSettled([resumenesPromise, clientesPromise, notificacionesPromise]);
      if (!getToken()) return;
      const errors = {};
      if (r.status === "fulfilled") setResumenes(r.value);
      else errors.tickets = "No se pudieron cargar los tickets. Intenta nuevamente; si continúa, contacta al administrador.";
      if (c.status === "fulfilled") { if (esAdmin) setClientes(c.value); }
      else errors.clientes = "No se pudieron cargar los clientes. Intenta nuevamente; si continúa, contacta al administrador.";
      if (n.status === "fulfilled") setUnreadCount(n.value.count || 0);
      setLoadErrors(errors);
    } finally {
      setLoading(false);
    }
  }, [sesionActiva, esAdmin]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    if (!sesionActiva) return undefined;
    const timer = window.setInterval(async () => {
      try {
        const data = await Api.notificaciones.unreadCount();
        setUnreadCount(data.count || 0);
      } catch {
        // El centro conserva su propio estado de error; el sondeo no interrumpe la pantalla actual.
      }
    }, 30000);
    return () => window.clearInterval(timer);
  }, [sesionActiva]);

  useEffect(() => {
    const onHashChange = () => setRouteState(parseHash());
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  useEffect(() => {
    function onUnauthorized() {
      setUsuario(null);
      setResumenes([]);
      setClientes([]);
      setUnreadCount(0);
      navigate("login");
    }
    window.addEventListener("gm:unauthorized", onUnauthorized);
    return () => window.removeEventListener("gm:unauthorized", onUnauthorized);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function navigate(nextRoute) {
    window.location.hash = "#" + nextRoute;
  }
  function openTicket(id) {
    window.location.hash = "#ticket/" + id;
  }

  function guardarSesion(cliente, irADashboard = true) {
    setUsuario(cliente);
    if (irADashboard) navigate(cliente.rol === "ADMINISTRADOR" ? "dashboard" : "resueltos");
  }

  async function cerrarSesion() {
    try {
      await Api.auth.logout();
    } catch {
      // aunque falle en el servidor, igual se limpia la sesión local
    }
    setUsuario(null);
    setResumenes([]);
    setClientes([]);
    setUnreadCount(0);
    navigate("login");
  }

  async function handleLogin(correo, password) {
    const cliente = await Api.auth.login(correo, password);
    guardarSesion(cliente);
  }
  async function handleRegister(data) {
    const cliente = await Api.auth.register(data);
    guardarSesion(cliente);
  }
  async function handleUpdatePerfil(data) {
    const actualizado = await Api.clientes.update(usuario.id, data);
    guardarSesion(actualizado, false);
    return actualizado;
  }

  async function handleCreateCliente(data) {
    const created = await Api.clientes.create(data);
    setClientes((prev) => [...prev, created]);
    return created;
  }
  async function handleRemoveCliente(id, password) {
    await Api.clientes.remove(id, password);
    if (usuario.id === id) {
      try { await Api.auth.logout(); } catch { /* la sesión ya fue invalidada por el servidor */ }
      setUsuario(null);
      setResumenes([]);
      setClientes([]);
      setUnreadCount(0);
      navigate("login");
      return;
    }
    await refresh();
  }
  async function handleUpdateCliente(id, data) {
    const actualizado = await Api.clientes.update(id, data);
    setClientes((prev) => prev.map((c) => c.id === id ? actualizado : c));
    setResumenes((prev) => prev.map((r) => r.cliente?.id === id ? { ...r, cliente: actualizado } : r));
    if (usuario.id === id) guardarSesion(actualizado, false);
    return actualizado;
  }
  async function handleCreateTicket(payload) {
    const created = await Api.resumenes.create(payload);
    await refresh();
    openTicket(created.id);
  }
  async function handleUpdateEstado(id, estado) {
    await Api.resumenes.updateEstado(id, estado);
    await refresh();
  }
  async function handleUpdateTicket(id, data) {
    await Api.resumenes.update(id, data);
    await refresh();
  }
  async function handleUpdateResena(id, data) {
    await Api.resumenes.updateResena(id, data);
    await refresh();
  }
  async function handlePublicarResena(id, data) {
    await Api.resumenes.publicarResena(id, data);
    await refresh();
  }
  async function handleUploadImagen(id, data) {
    await Api.resumenes.uploadImagen(id, data);
    await refresh();
  }
  async function handleRemoveImagen(resumenId, imagenId) {
    await Api.resumenes.removeImagen(resumenId, imagenId);
    await refresh();
  }
  async function handleDeleteTicket(id) {
    await Api.resumenes.remove(id);
    await refresh();
    navigate("tickets");
  }

  function openNotificationTarget(notificacion) {
    if (notificacion.ticketId) {
      navigate(`ticket/${notificacion.ticketId}${notificacion.tipo === "IMAGEN_TICKET_AGREGADA" ? "/imagenes" : ""}`);
    } else if (esAdmin && notificacion.clienteRelacionadoId) {
      navigate(`cliente/${notificacion.clienteRelacionadoId}`);
    }
  }

  // ----- sin sesión: solo login / registro -----
  if (!sesionActiva) {
    return route === "registro" ? (
      <RegisterView onRegister={handleRegister} onGoToLogin={() => navigate("login")} />
    ) : (
      <LoginView onLogin={handleLogin} onGoToRegister={() => navigate("registro")} />
    );
  }

  // "Clientes" y "Nuevo ticket" no existen para un USUARIO común, aunque
  // teclee el hash a mano. El backend igual las bloquea (ver AuthFilter /
  // servlets), esto es solo para no mostrarle una pantalla rota.
  const rutaBloqueada = !esAdmin && ["dashboard", "clientes", "nuevo"].includes(route);
  const rutaEfectiva = rutaBloqueada ? "resueltos" : route;
  const currentTicket = resumenes.find((r) => r.id === ticketId);
  const loadError = ["clientes", "nuevo"].includes(rutaEfectiva)
    ? loadErrors.clientes
    : loadErrors.tickets || (rutaEfectiva === "ticket" && esAdmin ? loadErrors.clientes : "");

  return (
    <div id="app">
      <Sidebar
        route={rutaEfectiva}
        onNavigate={navigate}
        usuario={usuario}
        esAdmin={esAdmin}
        onLogout={cerrarSesion}
        unreadCount={unreadCount}
      />
      <main className="main" id="contenido" tabIndex={-1}>
        {rutaEfectiva === "notificaciones" ? (
          <NotificacionesView onOpen={openNotificationTarget} onCountChange={setUnreadCount} />
        ) : rutaEfectiva === "resueltos" ? (
          <TrabajosResueltosView />
        ) : rutaEfectiva === "perfil" ? (
          <PerfilView usuario={usuario} onUpdate={handleUpdatePerfil} />
        ) : loading ? (
          <p className="hint">Cargando…</p>
        ) : loadError ? (
          <section className="card" aria-label="Error al cargar datos">
            <p role="alert">{loadError}</p>
            <button className="btn btn-primary" onClick={refresh}>Reintentar carga</button>
          </section>
        ) : rutaEfectiva === "dashboard" ? (
          <DashboardView
            resumenes={resumenes}
            onNavigate={navigate}
            onOpenTicket={openTicket}
            esAdmin={esAdmin}
            usuario={usuario}
          />
        ) : rutaEfectiva === "tickets" ? (
          <TicketsView resumenes={resumenes} onNavigate={navigate} onOpenTicket={openTicket} esAdmin={esAdmin} />
        ) : rutaEfectiva === "ticket" ? (
          <TicketDetailView
            resumen={currentTicket}
            esAdmin={esAdmin}
            onBack={() => navigate("tickets")}
            onUpdateEstado={handleUpdateEstado}
            onUpdateTicket={handleUpdateTicket}
            onUpdateResena={handleUpdateResena}
            onPublicarResena={handlePublicarResena}
            onUploadImagen={handleUploadImagen}
            onRemoveImagen={handleRemoveImagen}
            onDelete={handleDeleteTicket}
            clientes={clientes}
            focusSection={section}
          />
        ) : rutaEfectiva === "clientes" ? (
          <ClientesView
            clientes={clientes}
            resumenes={resumenes}
            ticketsError={loadErrors.tickets}
            onRetry={refresh}
            onCreate={handleCreateCliente}
            onUpdate={handleUpdateCliente}
            onRemove={handleRemoveCliente}
            onOpenTicket={openTicket}
            initialSelectedId={clientId}
          />
        ) : rutaEfectiva === "nuevo" ? (
          <NuevoTicketView
            clientes={clientes}
            onCreateCliente={handleCreateCliente}
            onCreateTicket={handleCreateTicket}
          />
        ) : null}
      </main>
    </div>
  );
}
