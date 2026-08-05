export function money(n) {
  return "$" + (Number(n) || 0).toLocaleString("es-MX", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function fecha(iso) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("es-MX", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

export function totalTicket(resumen) {
  return (resumen.listaTrabajos || []).reduce(
    (sum, t) => sum + (Number(t.precio) || 0),
    0
  );
}

function hashCode(str) {
  let h = 0;
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i);
    h |= 0;
  }
  return h;
}

// Genera un patrón de "código de barras" pseudo-aleatorio pero determinista
// a partir del id del ticket, puramente decorativo (etiqueta de taller).
export function barcodeStyle(seedStr) {
  const h = hashCode(String(seedStr));
  let stops = [];
  let pos = 0;
  for (let i = 0; i < 18; i++) {
    const w = 2 + ((h >> (i % 24)) & 3);
    const isBar = i % 2 === 0;
    stops.push(`${isBar ? "var(--ink)" : "transparent"} ${pos}px ${pos + w}px`);
    pos += w;
  }
  return {
    backgroundImage: `repeating-linear-gradient(90deg, ${stops.join(", ")})`,
    backgroundColor: "transparent",
    backgroundSize: `${pos}px 100%`,
  };
}
