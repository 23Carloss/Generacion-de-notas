package Service;

import DTOs.ClienteDTO;
import DTOs.DispositivoDTO;
import DTOs.ResumenDTO;
import DTOs.TrabajoDTO;
import hp.models.Cliente;
import hp.models.Dispositivo;
import hp.models.Resumen;
import hp.models.Trabajo;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Convierte entidades JPA a DTOs "planos" (sin relaciones inversas) para que
 * Jackson las pueda serializar sin toparse con proxies LAZY ni con
 * referencias circulares (Resumen -> Dispositivo -> Resumen).
 */
public class Mappers {

    private static final DateTimeFormatter FECHA_FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Mappers() {
    }

     public static ClienteDTO toDTO(Cliente c) {
        if (c == null) return null;
        ClienteDTO dto = new ClienteDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setTelefono(c.getTelefono());
        dto.setCorreo(c.getCorreo());
        if (c.getRol() != null) {
            dto.setRol(ClienteDTO.ROL.valueOf(c.getRol().name()));
        }
        // listaResumenes se omite a propósito: es LAZY y no la necesita el
        // front en ninguno de estos endpoints.
        // passwordHash/passwordSalt NUNCA se mapean al DTO.
        return dto;
    }
 
    // ------------------------------------------------------------ Dispositivo
    public static DispositivoDTO toDTO(Dispositivo d) {
        if (d == null) return null;
        DispositivoDTO dto = new DispositivoDTO();
        dto.setId(d.getId());
        dto.setModeloDispositivo(d.getModeloDispositivo());
        dto.setDetallesDispositivo(d.getDetallesDispositivo());
        if (d.getPlataforma() != null) {
            dto.setPlataforma(DispositivoDTO.Plataforma.valueOf(d.getPlataforma().name()));
        }
        // resumen se deja null a propósito, para no generar un ciclo.
        return dto;
    }
 
    // ---------------------------------------------------------------- Trabajo
    public static TrabajoDTO toDTO(Trabajo t) {
        if (t == null) return null;
        TrabajoDTO dto = new TrabajoDTO();
        dto.setId(t.getId());
        if (t.getTipoTrabajo() != null) {
            dto.setTipoTrabajo(TrabajoDTO.TipoTrabajo.valueOf(t.getTipoTrabajo().name()));
        }
        dto.setPrecio(t.getPrecio());
        return dto;
    }
 
    // ---------------------------------------------------------------- Resumen
    public static ResumenDTO toDTO(Resumen r, List<Dispositivo> dispositivos, List<Trabajo> trabajos) {
        if (r == null) return null;
        ResumenDTO dto = new ResumenDTO();
        dto.setId(r.getId());
        dto.setCliente(toDTO(r.getCliente()));
        dto.setComentariosCliente(r.getComentariosCliente());
        dto.setDescripcionProblema(r.getDescripcionProblema());
        dto.setEstado(estadoAFrontend(r.getEstado()));
        dto.setFechaCreacion(r.getFechaCreacion() != null ? r.getFechaCreacion().format(FECHA_FORMATO) : null);
        dto.setListaDispositivos(
                dispositivos == null ? List.of() :
                        dispositivos.stream().map(Mappers::toDTO).collect(Collectors.toList()));
        dto.setListaTrabajos(
                trabajos == null ? List.of() :
                        trabajos.stream().map(Mappers::toDTO).collect(Collectors.toList()));
        dto.setResenaComentario(r.getResenaComentario());
        dto.setCalificacion(r.getCalificacion());
        return dto;
    }
 
    // -------------------------------------------------- Mapeo de estado (enum <-> texto)
    // Debe coincidir EXACTO con ESTADOS en game-maintenance-frontend/src/constants.js
    public static String estadoAFrontend(Resumen.ESTADO estado) {
        if (estado == null) return "Recibido";
        switch (estado) {
            case Recibido: return "Recibido";
            case EnDiagnostico: return "En diagnóstico";
            case EnReparacion: return "En reparación";
            case ListoParaEntrega: return "Listo para entrega";
            case Entregado: return "Entregado";
            default: return estado.name();
        }
    }
 
    public static Resumen.ESTADO estadoDesdeFrontend(String estado) {
        if (estado == null) {
            throw new IllegalArgumentException("estado es obligatorio");
        }
        switch (estado) {
            case "Recibido": return Resumen.ESTADO.Recibido;
            case "En diagnóstico": return Resumen.ESTADO.EnDiagnostico;
            case "En reparación": return Resumen.ESTADO.EnReparacion;
            case "Listo para entrega": return Resumen.ESTADO.ListoParaEntrega;
            case "Entregado": return Resumen.ESTADO.Entregado;
            default: throw new IllegalArgumentException("Estado desconocido: " + estado);
        }
    }
}