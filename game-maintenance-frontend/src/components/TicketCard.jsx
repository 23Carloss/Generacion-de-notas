import Badge from "./Badge";
import { money, fecha, totalTicket, barcodeStyle } from "../utils";

export default function TicketCard({ resumen, onOpen }) {
  const cliente = resumen.cliente ? resumen.cliente.nombre : "Cliente";
  const dispositivos =
    (resumen.listaDispositivos || []).map((d) => d.modeloDispositivo).join(", ") ||
    "Sin dispositivo";

  return (
    <a
      className="tag-card"
      href="#"
      onClick={(e) => {
        e.preventDefault();
        onOpen(resumen.id);
      }}
    >
      <span className="tag-hole" />
      <div className="tag-id">TICKET #{resumen.id}</div>
      <div className="tag-name">{cliente}</div>
      <div className="tag-meta">{dispositivos}</div>
      <div className="tag-meta">{fecha(resumen.fechaCreacion)}</div>
      <div className="tag-divider" />
      <div className="tag-barcode" style={barcodeStyle("T" + resumen.id)} />
      <div className="tag-bottom">
        <Badge estado={resumen.estado} />
        <span className="tag-price">{money(totalTicket(resumen))}</span>
      </div>
    </a>
  );
}
