// Estrellas de calificación (1-5), reutilizadas tanto en modo solo-lectura
// (TicketCard, resumen para el admin) como en modo interactivo (formulario
// de reseña del usuario). Usa las clases .estrellas / .estrella / .llena y
// .estrellas-input ya definidas en index.css.

export default function Stars({ value = 0, onChange, size }) {
  const estrellas = [1, 2, 3, 4, 5];
  const readOnly = !onChange;

  if (readOnly) {
    return (
      <span className="estrellas" style={size ? { fontSize: size } : undefined}>
        {estrellas.map((n) => (
          <span key={n} className={"estrella" + (n <= value ? " llena" : "")}>
            ★
          </span>
        ))}
      </span>
    );
  }

  return (
    <span className="estrellas estrellas-input" style={size ? { fontSize: size } : undefined}>
      {estrellas.map((n) => (
        <button
          key={n}
          type="button"
          className={n <= value ? "llena" : ""}
          onClick={() => onChange(n)}
          aria-label={`Calificar con ${n} estrella${n === 1 ? "" : "s"}`}
        >
          ★
        </button>
      ))}
    </span>
  );
}
