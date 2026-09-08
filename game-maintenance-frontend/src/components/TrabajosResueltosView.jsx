import { useEffect, useId, useState } from "react";
import { Api } from "../api";
import { PLATAFORMAS, TIPOS_TRABAJO } from "../constants";
import Stars from "./Stars";

export default function TrabajosResueltosView({ compact = false, onNavigate }) {
  const [pagina, setPagina] = useState(1);
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const heading = useId();
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    Api.trabajosResueltos.list(pagina, compact ? 3 : 12).then((data) => {
      if (active) setResult(data);
    }).catch((e) => {
      if (active) setError(e.message || "No se pudieron cargar los trabajos.");
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [pagina, compact, attempt]);

  const Heading = compact ? "h2" : "h1";
  return (
    <section className={"portfolio" + (compact ? " portfolio-compact" : "")} aria-labelledby={heading}>
      <div className="view-header">
        <div>
          <p className="eyebrow">La experiencia de nuestro taller</p>
          <Heading id={heading}>Trabajos resueltos</Heading>
          <p>Equipos entregados y opiniones de clientes. Sin nombres ni datos de contacto.</p>
        </div>
        {compact && <button className="btn btn-ghost" onClick={() => onNavigate("resueltos")}>Explorar trabajos →</button>}
      </div>
      {loading ? <p className="hint" role="status">Cargando trabajos resueltos…</p> : error ? (
        <div className="card"><p role="alert">{error}</p><button className="btn btn-ghost" onClick={() => setAttempt(attempt + 1)}>Reintentar</button></div>
      ) : result && <>
        <div className="portfolio-summary" aria-label="Experiencia del taller">
          <span><strong>{result.total}</strong> trabajos entregados</span>
          <span><strong>{result.valoraciones ? Number(result.promedio).toFixed(1) + " / 5" : "—"}</strong> {result.valoraciones} valoraciones</span>
          <span className="privacy-note">Vista anónima · Solo usuarios con sesión</span>
        </div>
        {result.trabajos.length ? <div className="portfolio-grid">
          {result.trabajos.map((trabajo, index) => (
            <article className="portfolio-card" key={`${pagina}-${index}`}>
              <div className="portfolio-card-top"><span className="portfolio-icon" aria-hidden="true">✓</span><span className="badge badge-entregado">Entregado</span></div>
              <h3>{trabajo.plataformas.map((p) => PLATAFORMAS[p]).filter(Boolean).join(" · ") || "Equipo atendido"}</h3>
              <div className="service-tags">{trabajo.servicios.length ? trabajo.servicios.map((s) => <span key={s}>{TIPOS_TRABAJO[s] || "Servicio técnico"}</span>) : <span>Servicio técnico</span>}</div>
              <div className="portfolio-review">
                {trabajo.calificacion ? <div aria-label={`Calificación: ${trabajo.calificacion} de 5 estrellas`}><Stars value={trabajo.calificacion} /></div> : <p className="hint">Aún sin valoración</p>}
                {trabajo.resena ? <><blockquote>“{trabajo.resena}”</blockquote><p className="hint">Fragmento de una reseña de cliente · Identidad privada</p></> : <p className="hint">Sin comentario público.</p>}
              </div>
            </article>
          ))}
        </div> : <div className="empty-state"><h2>{result.total ? "No hay trabajos en esta página" : "Los próximos resultados estarán aquí"}</h2><p>Solo se muestran los tickets marcados como Entregado.</p></div>}
        {!compact && <nav className="portfolio-pagination" aria-label="Páginas de trabajos resueltos">
          <button className="btn btn-ghost" disabled={pagina === 1} onClick={() => setPagina(pagina - 1)}>← Anterior</button>
          <span role="status">Página {pagina} de {Math.max(pagina, Math.ceil(result.total / 12))}</span>
          <button className="btn btn-ghost" disabled={pagina * 12 >= result.total} onClick={() => setPagina(pagina + 1)}>Siguiente →</button>
        </nav>}
      </>}
    </section>
  );
}
