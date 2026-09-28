package Service;

import ConexionDB.ManejadorConexiones;
import DTOs.NotificacionDTO;
import DTOs.PaginaNotificacionesDTO;
import Exceptions.PersistenciaException;
import hp.models.Cliente;
import hp.models.Imagen;
import hp.models.Notificacion;
import hp.models.Resumen;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;

/** Casos de uso y fábrica transaccional del centro de notificaciones. */
public class NotificacionService {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public PaginaNotificacionesDTO listar(Long destinatarioId, Boolean soloNoLeidas, int pagina, int tamano)
            throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            String filtro = soloNoLeidas == null ? ""
                    : (soloNoLeidas ? " AND n.leida = false" : " AND n.leida = true");
            List<Notificacion> items = em.createQuery(
                    "SELECT n FROM Notificacion n WHERE n.destinatario.id = :id" + filtro
                    + " ORDER BY n.fechaCreacion DESC, n.id DESC", Notificacion.class)
                    .setParameter("id", destinatarioId)
                    .setFirstResult(Math.multiplyExact(pagina, tamano))
                    .setMaxResults(tamano)
                    .getResultList();
            long total = em.createQuery(
                    "SELECT COUNT(n) FROM Notificacion n WHERE n.destinatario.id = :id" + filtro, Long.class)
                    .setParameter("id", destinatarioId).getSingleResult();
            return new PaginaNotificacionesDTO(items.stream().map(this::toDTO).collect(Collectors.toList()),
                    pagina, tamano, total, contarNoLeidas(em, destinatarioId));
        } catch (ArithmeticException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar las notificaciones: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public long contarNoLeidas(Long destinatarioId) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return contarNoLeidas(em, destinatarioId);
        } catch (Exception e) {
            throw new PersistenciaException("Error al contar las notificaciones: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public NotificacionDTO marcarLeida(Long notificacionId, Long destinatarioId) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            List<Notificacion> encontradas = em.createQuery(
                    "SELECT n FROM Notificacion n WHERE n.id = :notificacionId AND n.destinatario.id = :destinatarioId",
                    Notificacion.class)
                    .setParameter("notificacionId", notificacionId)
                    .setParameter("destinatarioId", destinatarioId)
                    .setMaxResults(1).getResultList();
            if (encontradas.isEmpty()) {
                em.getTransaction().rollback();
                return null;
            }
            Notificacion notificacion = encontradas.get(0);
            notificacion.setLeida(true);
            em.getTransaction().commit();
            return toDTO(notificacion);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new PersistenciaException("Error al marcar la notificación: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public int marcarTodasLeidas(Long destinatarioId) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            int actualizadas = em.createQuery(
                    "UPDATE Notificacion n SET n.leida = true WHERE n.destinatario.id = :id AND n.leida = false")
                    .setParameter("id", destinatarioId).executeUpdate();
            em.getTransaction().commit();
            return actualizadas;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new PersistenciaException("Error al marcar las notificaciones: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    private long contarNoLeidas(EntityManager em, Long destinatarioId) {
        return em.createQuery(
                "SELECT COUNT(n) FROM Notificacion n WHERE n.destinatario.id = :id AND n.leida = false", Long.class)
                .setParameter("id", destinatarioId).getSingleResult();
    }

    private NotificacionDTO toDTO(Notificacion n) {
        NotificacionDTO dto = new NotificacionDTO();
        dto.setId(n.getId());
        dto.setTipo(n.getTipo().name());
        dto.setTitulo(n.getTitulo());
        dto.setMensaje(n.getMensaje());
        dto.setLeida(n.isLeida());
        dto.setFechaCreacion(n.getFechaCreacion().format(FECHA));
        dto.setTicketId(n.getResumen() == null ? null : n.getResumen().getId());
        dto.setImagenId(n.getImagen() == null ? null : n.getImagen().getId());
        dto.setClienteRelacionadoId(n.getClienteRelacionadoId());
        return dto;
    }

    public static void notificarRegistro(EntityManager em, Cliente registrado) {
        for (Cliente admin : administradores(em)) {
            crear(em, admin, Notificacion.TipoNotificacion.USUARIO_REGISTRADO,
                    "Nuevo usuario registrado", registrado.getNombre() + " completó su registro.",
                    null, null, registrado.getId());
        }
    }

    public static void notificarCambioEstado(EntityManager em, Resumen resumen,
            Resumen.ESTADO anterior, Resumen.ESTADO nuevo) {
        String mensajeAdmin = "El ticket #" + resumen.getId() + " cambió de \""
                + Mappers.estadoAFrontend(anterior) + "\" a \"" + Mappers.estadoAFrontend(nuevo) + "\".";
        Set<Long> destinatarios = new LinkedHashSet<>();
        for (Cliente admin : administradores(em)) {
            if (destinatarios.add(admin.getId())) {
                crear(em, admin, Notificacion.TipoNotificacion.ESTADO_TICKET_CAMBIADO,
                        "Estado de ticket actualizado", mensajeAdmin, resumen, null, null);
            }
        }
        Cliente propietario = resumen.getCliente();
        if (propietario != null && destinatarios.add(propietario.getId())) {
            crear(em, propietario, Notificacion.TipoNotificacion.ESTADO_TICKET_CAMBIADO,
                    "Tu ticket cambió de estado", "El estado de tu ticket #" + resumen.getId()
                    + " cambió de \"" + Mappers.estadoAFrontend(anterior) + "\" a \""
                    + Mappers.estadoAFrontend(nuevo) + "\".", resumen, null, null);
        }
    }

    public static void notificarResena(EntityManager em, Resumen resumen) {
        for (Cliente admin : administradores(em)) {
            crear(em, admin, Notificacion.TipoNotificacion.RESENA_CREADA,
                    "Nueva reseña", "Se publicó una nueva reseña para el ticket #" + resumen.getId() + ".",
                    resumen, null, resumen.getCliente() == null ? null : resumen.getCliente().getId());
        }
    }

    public static void notificarImagen(EntityManager em, Resumen resumen, Imagen imagen) {
        if (resumen.getCliente() != null) {
            crear(em, resumen.getCliente(), Notificacion.TipoNotificacion.IMAGEN_TICKET_AGREGADA,
                    "Nueva foto en tu ticket", "Se agregó una fotografía al ticket #" + resumen.getId() + ".",
                    resumen, imagen, null);
        }
    }

    private static List<Cliente> administradores(EntityManager em) {
        return em.createQuery("SELECT c FROM Cliente c WHERE c.rol = :rol", Cliente.class)
                .setParameter("rol", Cliente.ROL.ADMINISTRADOR).getResultList();
    }

    private static void crear(EntityManager em, Cliente destinatario, Notificacion.TipoNotificacion tipo,
            String titulo, String mensaje, Resumen resumen, Imagen imagen, Long clienteRelacionadoId) {
        Notificacion n = new Notificacion();
        n.setDestinatario(destinatario);
        n.setTipo(tipo);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        n.setResumen(resumen);
        n.setImagen(imagen);
        n.setClienteRelacionadoId(clienteRelacionadoId);
        em.persist(n);
    }
}
