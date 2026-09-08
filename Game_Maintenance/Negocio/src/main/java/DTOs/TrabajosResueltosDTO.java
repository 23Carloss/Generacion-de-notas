package DTOs;

import java.util.List;

/** Explicit public allowlist: never serialize ResumenDTO or a JPA entity here. */
public record TrabajosResueltosDTO(List<TrabajoResuelto> trabajos, long total,
        int pagina, int tamano, long valoraciones, Double promedio) {
    public record TrabajoResuelto(List<String> plataformas, List<String> servicios,
            Integer calificacion, String resena) {}
}
