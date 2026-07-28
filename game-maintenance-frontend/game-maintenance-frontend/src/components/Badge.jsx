import { ESTADO_BADGE_CLASS } from "../constants";

export default function Badge({ estado, large }) {
  const cls = ESTADO_BADGE_CLASS[estado] || "badge-recibido";
  return (
    <span
      className={`badge ${cls}`}
      style={large ? { fontSize: ".75rem", padding: ".4rem .8rem" } : undefined}
    >
      <span className="dot" />
      {estado}
    </span>
  );
}
