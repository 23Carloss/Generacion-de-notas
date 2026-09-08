package Service;

import ConexionDB.ManejadorConexiones;
import DTOs.TrabajosResueltosDTO;
import DTOs.TrabajosResueltosDTO.TrabajoResuelto;
import hp.models.Resumen;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

public final class TrabajosResueltosService {
    private static final Pattern CONTACTO = Pattern.compile(
            "(?i)(@|https?://|www\\.|\\b[\\w.-]+\\.(com|mx|net|org|test)\\b|\\d(?:[\\s()+.-]*\\d){6,}|[<>])");

    public TrabajosResueltosDTO listar(int pagina, int tamano) {
        if (pagina < 1 || pagina > 100_000 || tamano < 1 || tamano > 20)
            throw new IllegalArgumentException("Página inválida: usa pagina de 1 a 100000 y tamano de 1 a 20.");
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            Object[] stats = em.createQuery("SELECT COUNT(r), COUNT(r.calificacion), AVG(r.calificacion) "
                    + "FROM Resumen r WHERE r.estado = :estado", Object[].class)
                    .setParameter("estado", Resumen.ESTADO.Entregado).getSingleResult();
            // No names, dates, prices, images, free-form device data or private comments are fetched.
            List<Object[]> rows = em.createQuery("SELECT r.id, r.calificacion, r.resenaPublica "
                    + "FROM Resumen r WHERE r.estado = :estado ORDER BY r.id DESC", Object[].class)
                    .setParameter("estado", Resumen.ESTADO.Entregado)
                    .setFirstResult((pagina - 1) * tamano).setMaxResults(tamano).getResultList();
            Map<Long, List<String>> plataformas = new HashMap<>(), servicios = new HashMap<>();
            if (!rows.isEmpty()) {
                List<Long> ids = rows.stream().map(r -> (Long) r[0]).toList();
                agrupar(em.createQuery("SELECT DISTINCT d.resumen.id, d.plataforma FROM Dispositivo d "
                        + "WHERE d.resumen.id IN :ids", Object[].class).setParameter("ids", ids).getResultList(), plataformas);
                agrupar(em.createQuery("SELECT DISTINCT t.resumen.id, t.tipoTrabajo FROM Trabajo t "
                        + "WHERE t.resumen.id IN :ids", Object[].class).setParameter("ids", ids).getResultList(), servicios);
            }
            List<TrabajoResuelto> trabajos = rows.stream().map(r -> new TrabajoResuelto(
                    plataformas.getOrDefault(r[0], List.of()), servicios.getOrDefault(r[0], List.of()),
                    (Integer) r[1], (String) r[2])).toList();
            return new TrabajosResueltosDTO(trabajos, (Long) stats[0], pagina, tamano, (Long) stats[1], (Double) stats[2]);
        } finally { em.close(); }
    }

    private static void agrupar(List<Object[]> rows, Map<Long, List<String>> destino) {
        for (Object[] row : rows) if (row[1] != null)
            destino.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add(((Enum<?>) row[1]).name());
        destino.values().forEach(Collections::sort);
    }

    /** Called exclusively after the servlet's administrator check. Null text unpublishes. */
    public boolean publicar(Long id, String texto, String original, boolean confirmado) {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen r = em.find(Resumen.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (r == null) { em.getTransaction().rollback(); return false; }
            String extracto = texto == null || texto.isBlank() ? null : texto.trim();
            if (extracto != null) {
                if (!confirmado || r.getEstado() != Resumen.ESTADO.Entregado || r.getCalificacion() == null)
                    throw new IllegalArgumentException("Revisa la privacidad de una reseña de un ticket entregado antes de publicar.");
                if (!Objects.equals(original, r.getResenaComentario()))
                    throw new IllegalArgumentException("La reseña cambió. Recarga el ticket y vuelve a revisarla.");
                if (extracto.length() > 600 || original == null || !original.contains(extracto))
                    throw new IllegalArgumentException("Usa un fragmento literal de la reseña original de hasta 600 caracteres, sin cambiar su sentido.");
                if (CONTACTO.matcher(extracto).find() || contieneNombre(extracto, r))
                    throw new IllegalArgumentException("El fragmento puede contener datos personales, enlaces o etiquetas. Selecciona otro fragmento.");
            }
            r.setResenaPublica(extracto);
            em.getTransaction().commit();
            return true;
        } finally {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            em.close();
        }
    }

    private static boolean contieneNombre(String texto, Resumen r) {
        if (r.getCliente() == null || r.getCliente().getNombre() == null) return false;
        Set<String> palabras = new HashSet<>(Arrays.asList(normalizar(texto).split("[^a-z]+")));
        return Arrays.stream(normalizar(r.getCliente().getNombre()).split("[^a-z]+"))
                .filter(p -> p.length() >= 3).anyMatch(palabras::contains);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
