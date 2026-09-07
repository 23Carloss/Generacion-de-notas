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
import { Api, getToken } from "./api";

function parseHash() {
  const h = window.location.hash.replace(/^#\/?/, "");
  if (h.startsWith("ticket/")) {
    return { route: "ticket", ticketId: Number(h.split("/")[1]) };
  }
  if (["dashboard", "nuevo", "tickets", "clientes", "perfil", "login", "registro"].includes(h)) {
    return { route: h, ticketId: null };
  }
  return { route: "dashboard", ticketId: null };
}

export default function App() {
  const [{ route, ticketId }, setRouteState] = useState(parseHash());
  const [usuario, setUsuario] = useState(null);
  const [clientes, setClientes] = useState([]);
  const [resumenes, setResumenes] = useState([]);
  const [loading, setLoading] = useState(true);

  // sesionActiva revisa tanto el usuario guardado como que exista un token:
  // si el token se limpió (por un 401) pero el usuario seguía en el estado
  // de React, no queremos tratarlo como "logueado".
  const sesionActiva = !!usuario && !!getToken();
  const esAdmin = usuario?.rol === "ADMINISTRADOR";

  const refresh = useCallback(async () => {
    if (!sesionActiva) return;
    setLoading(true);
    try {
      const resumenesPromise = Api.resumenes.list();
      const clientesPromise = esAdmin ? Api.clientes.list() : Promise.resolve([]);
      const [r, c] = await Promise.all([resumenesPromise, clientesPromise]);
      setResumenes(r);
      if (esAdmin) setClientes(c);
    } catch {
      // Si fue un 401, el listener de "gm:unauthorized" ya se encarga de
      // cerrar la sesión y regresar al login; cualquier otro error se
      // ignora aquí por ahora (no hay una UI de error global todavía).
    } finally {
      setLoading(false);
    }
  }, [sesionActiva, esAdmin]);

  useEffect(() => {
    refresh();
  }, [refresh]);

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
    if (irADashboard) navigate("dashboard");
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
  async function handleRemoveCliente(id) {
    await Api.clientes.remove(id);
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
  const rutaBloqueada = !esAdmin && (route === "clientes" || route === "nuevo");
  const rutaEfectiva = rutaBloqueada ? "dashboard" : route;
  const currentTicket = resumenes.find((r) => r.id === ticketId);

  return (
    <div id="app">
      <Sidebar
        route={rutaEfectiva}
        onNavigate={navigate}
        usuario={usuario}
        esAdmin={esAdmin}
        onLogout={cerrarSesion}
      />
      <main className="main">
        {loading ? (
          <p className="hint">Cargando…</p>
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
            onUploadImagen={handleUploadImagen}
            onRemoveImagen={handleRemoveImagen}
            onDelete={handleDeleteTicket}
            clientes={clientes}
          />
        ) : rutaEfectiva === "clientes" ? (
          <ClientesView
            clientes={clientes}
            resumenes={resumenes}
            onCreate={handleCreateCliente}
            onUpdate={handleUpdateCliente}
            onRemove={handleRemoveCliente}
            onOpenTicket={openTicket}
          />
        ) : rutaEfectiva === "nuevo" ? (
          <NuevoTicketView
            clientes={clientes}
            onCreateCliente={handleCreateCliente}
            onCreateTicket={handleCreateTicket}
          />
        ) : rutaEfectiva === "perfil" ? (
          <PerfilView usuario={usuario} onUpdate={handleUpdatePerfil} />
        ) : null}
      </main>
    </div>
  );
}
