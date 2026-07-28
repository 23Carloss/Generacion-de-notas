import { useEffect, useState, useCallback } from "react";
import Sidebar from "./components/Sidebar";
import DashboardView from "./components/DashboardView";
import TicketsView from "./components/TicketsView";
import TicketDetailView from "./components/TicketDetailView";
import ClientesView from "./components/ClientesView";
import NuevoTicketView from "./components/NuevoTicketView";
import { Api } from "./api";

function parseHash() {
  const h = window.location.hash.replace(/^#\/?/, "");
  if (h.startsWith("ticket/")) {
    return { route: "ticket", ticketId: Number(h.split("/")[1]) };
  }
  if (["dashboard", "nuevo", "tickets", "clientes"].includes(h)) {
    return { route: h, ticketId: null };
  }
  return { route: "dashboard", ticketId: null };
}

export default function App() {
  const [{ route, ticketId }, setRouteState] = useState(parseHash());
  const [clientes, setClientes] = useState([]);
  const [resumenes, setResumenes] = useState([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const [c, r] = await Promise.all([Api.clientes.list(), Api.resumenes.list()]);
    setClientes(c);
    setResumenes(r);
    setLoading(false);
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    const onHashChange = () => setRouteState(parseHash());
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  function navigate(nextRoute) {
    window.location.hash = "#" + nextRoute;
  }
  function openTicket(id) {
    window.location.hash = "#ticket/" + id;
  }

  async function handleCreateCliente(data) {
    const created = await Api.clientes.create(data);
    await refresh();
    return created;
  }
  async function handleRemoveCliente(id) {
    await Api.clientes.remove(id);
    await refresh();
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
  async function handleDeleteTicket(id) {
    await Api.resumenes.remove(id);
    await refresh();
    navigate("tickets");
  }

  const currentTicket = resumenes.find((r) => r.id === ticketId);

  return (
    <div id="app">
      <Sidebar route={route} onNavigate={navigate} />
      <main className="main">
        {loading ? (
          <p className="hint">Cargando…</p>
        ) : route === "dashboard" ? (
          <DashboardView resumenes={resumenes} onNavigate={navigate} onOpenTicket={openTicket} />
        ) : route === "tickets" ? (
          <TicketsView resumenes={resumenes} onNavigate={navigate} onOpenTicket={openTicket} />
        ) : route === "ticket" ? (
          <TicketDetailView
            resumen={currentTicket}
            onBack={() => navigate("tickets")}
            onUpdateEstado={handleUpdateEstado}
            onDelete={handleDeleteTicket}
          />
        ) : route === "clientes" ? (
          <ClientesView
            clientes={clientes}
            resumenes={resumenes}
            onCreate={handleCreateCliente}
            onRemove={handleRemoveCliente}
          />
        ) : route === "nuevo" ? (
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
