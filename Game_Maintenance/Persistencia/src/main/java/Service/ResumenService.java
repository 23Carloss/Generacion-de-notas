package Service;

import ConexionDB.ManejadorConexiones;
import DAOs.DispositivoDAO;
import DAOs.ImagenDAO;
import DAOs.TrabajoDAO;
import DTOs.DispositivoDTO;
import DTOs.ImagenDTO;
import DTOs.ResumenDTO;
import DTOs.TrabajoDTO;
import Exceptions.PersistenciaException;
import Exceptions.AutorizacionException;
import hp.models.Cliente;
import hp.models.Dispositivo;
import hp.models.Imagen;
import hp.models.Resumen;
import hp.models.Trabajo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServlet;

/**
 * Orquesta la creación/lectura/actualización/borrado de un ticket (Resumen)
 * junto con sus Dispositivos y Trabajos relacionados, en una sola transacción
 * cuando corresponde (los DAOs individuales usan una transacción por llamada,
 * lo cual no alcanza para crear/eliminar un ticket completo de forma atómica).
 */

public class ResumenService extends HttpServlet {
    private static final int MAX_IMAGE_BYTES = 1_048_576;
    private static final Pattern DATA_URL_IMAGE = Pattern.compile(
            "^data:image/(png|jpeg|webp);base64,[A-Za-z0-9+/=]+$");
 
    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private final TrabajoDAO trabajoDAO = new TrabajoDAO();
    private final ImagenDAO imagenDAO = new ImagenDAO();
 
    public List<ResumenDTO> listarResumenesDTO() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Resumen> resumenes = em.createQuery("SELECT r FROM Resumen r", Resumen.class).getResultList();
            return mapearYOrdenar(resumenes);
        } catch (PersistenciaException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los tickets: " + e.getMessage());
        } finally {
            em.close();
        }
    }
 
    /**
     * Igual que listarResumenesDTO(), pero solo con los tickets de un
     * cliente en particular. Lo usa ResumenServlet cuando quien pregunta es
     * un USUARIO (no un ADMINISTRADOR).
     */
    public List<ResumenDTO> listarResumenesDTOPorCliente(Long idCliente) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Resumen> resumenes = em.createQuery(
                    "SELECT r FROM Resumen r WHERE r.cliente.id = :idCliente", Resumen.class)
                    .setParameter("idCliente", idCliente)
                    .getResultList();
            return mapearYOrdenar(resumenes);
        } catch (PersistenciaException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los tickets del cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }
 
    private List<ResumenDTO> mapearYOrdenar(List<Resumen> resumenes) throws PersistenciaException {
        resumenes.sort(Comparator.comparing(Resumen::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())));
 
        List<ResumenDTO> dtos = new ArrayList<>();
        for (Resumen r : resumenes) {
            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(r.getId());
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(r.getId());
            List<Imagen> imagenes = imagenDAO.listarPorResumen(r.getId());
            dtos.add(Mappers.toDTO(r, dispositivos, trabajos, imagenes));
        }
        return dtos;
    }
 
    public ResumenDTO obtenerResumenDTO(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            Resumen r = em.find(Resumen.class, id);
            if (r == null) return null;
            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(id);
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(id);
            List<Imagen> imagenes = imagenDAO.listarPorResumen(id);
            return Mappers.toDTO(r, dispositivos, trabajos, imagenes);
        } finally {
            em.close();
        }
    }
 
    public ResumenDTO crearResumenCompleto(ResumenDTO entrada) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
 
            Cliente cliente = em.find(Cliente.class, entrada.getCliente().getId());
            if (cliente == null) {
                em.getTransaction().rollback();
                throw new IllegalArgumentException("El cliente indicado no existe");
            }
 
            String descripcionProblema = Util.ValidationUtil.requiredText(
                    entrada.getDescripcionProblema(), "descripcionProblema", 2_000);
            String comentariosCliente = textoOpcional(entrada.getComentariosCliente(), "comentariosCliente", 2_000);

            Resumen resumen = new Resumen();
            resumen.setCliente(cliente);
            resumen.setDescripcionProblema(descripcionProblema);
            resumen.setComentariosCliente(comentariosCliente);
            resumen.setEstado(Resumen.ESTADO.Recibido);
            resumen.setFechaCreacion(LocalDateTime.now());
            em.persist(resumen);
 
            List<Dispositivo> dispositivosCreados = crearDispositivos(em, resumen, entrada.getListaDispositivos());
 
            List<Trabajo> trabajosCreados = crearTrabajos(em, resumen, entrada.getListaTrabajos());
 
            em.getTransaction().commit();
            return Mappers.toDTO(resumen, dispositivosCreados, trabajosCreados, List.of());
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al crear el ticket: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /** Actualiza los datos editables de un ticket. Solo se invoca desde el servlet tras validar el rol ADMINISTRADOR. */
    public ResumenDTO actualizarResumenCompleto(Long id, ResumenDTO entrada) throws PersistenciaException {
        if (entrada == null || entrada.getCliente() == null || entrada.getCliente().getId() == null) {
            throw new IllegalArgumentException("cliente.id es obligatorio");
        }
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, id, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (resumen == null) {
                em.getTransaction().rollback();
                return null;
            }
            Cliente cliente = em.find(Cliente.class, entrada.getCliente().getId());
            if (cliente == null) {
                throw new IllegalArgumentException("El cliente indicado no existe");
            }

            if (resumen.getCliente() == null || !cliente.getId().equals(resumen.getCliente().getId())) {
                resumen.setResenaComentario(null);
                resumen.setCalificacion(null);
            }
            resumen.setCliente(cliente);
            resumen.setDescripcionProblema(Util.ValidationUtil.requiredText(
                    entrada.getDescripcionProblema(), "descripcionProblema", 2_000));
            resumen.setComentariosCliente(textoOpcional(entrada.getComentariosCliente(), "comentariosCliente", 2_000));
            resumen.setResenaPublica(null);

            em.createQuery("DELETE FROM Dispositivo d WHERE d.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM Trabajo t WHERE t.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();

            List<Dispositivo> dispositivos = crearDispositivos(em, resumen, entrada.getListaDispositivos());
            List<Trabajo> trabajos = crearTrabajos(em, resumen, entrada.getListaTrabajos());
            em.getTransaction().commit();

            List<Imagen> imagenes = imagenDAO.listarPorResumen(id);
            return Mappers.toDTO(resumen, dispositivos, trabajos, imagenes);
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el ticket: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    private List<Dispositivo> crearDispositivos(EntityManager em, Resumen resumen, List<DispositivoDTO> entradas) {
        List<Dispositivo> dispositivos = new ArrayList<>();
        if (entradas == null || entradas.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un dispositivo.");
        }
        for (DispositivoDTO d : entradas) {
            if (d == null || d.getPlataforma() == null) {
                throw new IllegalArgumentException("El dispositivo contiene datos inválidos.");
            }
            Dispositivo dispositivo = new Dispositivo();
            dispositivo.setModeloDispositivo(Util.ValidationUtil.requiredText(
                    d.getModeloDispositivo(), "modeloDispositivo", 50));
            dispositivo.setDetallesDispositivo(textoOpcional(
                    d.getDetallesDispositivo(), "detallesDispositivo", 150));
            dispositivo.setPlataforma(Dispositivo.Plataforma.valueOf(d.getPlataforma().name()));
            dispositivo.setResumen(resumen);
            em.persist(dispositivo);
            dispositivos.add(dispositivo);
        }
        return dispositivos;
    }

    private List<Trabajo> crearTrabajos(EntityManager em, Resumen resumen, List<TrabajoDTO> entradas) {
        List<Trabajo> trabajos = new ArrayList<>();
        if (entradas == null) {
            return trabajos;
        }
        for (TrabajoDTO t : entradas) {
            if (t == null || t.getTipoTrabajo() == null) {
                throw new IllegalArgumentException("El trabajo contiene datos inválidos.");
            }
            Trabajo trabajo = new Trabajo();
            Trabajo.TipoTrabajo tipo = Trabajo.TipoTrabajo.valueOf(t.getTipoTrabajo().name());
            trabajo.setTipoTrabajo(tipo);
            if (tipo == Trabajo.TipoTrabajo.REPARACION) {
                if (t.getUnidades() == null || t.getUnidades() < 1 || t.getUnidades() > 50
                        || !precioValido(t.getPrecioUnitario()) || t.getPrecioUnitario() > 50_000) {
                    throw new IllegalArgumentException("La reparación permite de 1 a 50 unidades y un precio unitario entre 0 y 50,000 MXN.");
                }
                trabajo.setNombrePieza(Util.ValidationUtil.requiredText(t.getNombrePieza(), "nombrePieza", 75));
                trabajo.setUnidades(t.getUnidades());
                trabajo.setPrecioUnitario(t.getPrecioUnitario());
                double total = t.getUnidades() * t.getPrecioUnitario();
                if (!Double.isFinite(total) || total > 2_500_000) {
                    throw new IllegalArgumentException("El precio total de la reparación excede el límite permitido.");
                }
                trabajo.setPrecio(total);
            } else {
                if (!precioValido(t.getPrecio())) {
                    throw new IllegalArgumentException("El trabajo contiene datos inválidos.");
                }
                trabajo.setPrecio(t.getPrecio());
                trabajo.setNombrePieza(null);
                trabajo.setUnidades(null);
                trabajo.setPrecioUnitario(null);
            }
            trabajo.setResumen(resumen);
            em.persist(trabajo);
            trabajos.add(trabajo);
        }
        return trabajos;
    }

    private boolean precioValido(Double precio) {
        return precio != null && Double.isFinite(precio) && precio >= 0 && precio <= 1_000_000;
    }

    private String textoOpcional(String texto, String campo, int longitudMaxima) {
        return texto == null || texto.isBlank() ? null
                : Util.ValidationUtil.requiredText(texto, campo, longitudMaxima);
    }
 
    public ResumenDTO actualizarEstado(Long id, String estadoFrontend) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, id, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (resumen == null) {
                em.getTransaction().rollback();
                return null;
            }
            resumen.setEstado(Mappers.estadoDesdeFrontend(estadoFrontend));
            if (resumen.getEstado() != Resumen.ESTADO.Entregado) resumen.setResenaPublica(null);
            em.getTransaction().commit();
 
            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(id);
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(id);
            List<Imagen> imagenes = imagenDAO.listarPorResumen(id);
            return Mappers.toDTO(resumen, dispositivos, trabajos, imagenes);
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el estado: " + e.getMessage());
        } finally {
            em.close();
        }
    }
 
    /**
     * Guarda la reseña/calificación que deja el cliente sobre un ticket.
     * Reglas de negocio (pedidas explícitamente en el plan de trabajo):
     *   - Solo el cliente dueño del ticket puede reseñarlo (AutorizacionException si no).
     *   - Solo si el ticket ya está en estado Entregado (IllegalArgumentException si no).
     * Se puede volver a llamar para actualizar una reseña ya existente,
     * mientras el ticket siga Entregado (que es un estado final).
     */
    public ResumenDTO actualizarResena(Long idResumen, Long callerId, String comentario, Integer calificacion)
            throws PersistenciaException, AutorizacionException {
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            throw new IllegalArgumentException("la calificación debe ser un número entre 1 y 5");
        }
 
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, idResumen, javax.persistence.LockModeType.PESSIMISTIC_WRITE);
            if (resumen == null) {
                em.getTransaction().rollback();
                return null;
            }
            if (resumen.getCliente() == null || callerId == null
                    || !resumen.getCliente().getId().equals(callerId)) {
                em.getTransaction().rollback();
                throw new AutorizacionException("solo el cliente dueño del ticket puede reseñarlo");
            }
            if (resumen.getEstado() != Resumen.ESTADO.Entregado) {
                em.getTransaction().rollback();
                throw new IllegalArgumentException(
                        "solo se puede reseñar un ticket cuando su estado es Entregado");
            }
 
            resumen.setResenaComentario(comentario == null || comentario.isBlank() ? null
                    : Util.ValidationUtil.requiredText(comentario, "resenaComentario", 1_000));
            resumen.setCalificacion(calificacion);
            resumen.setResenaPublica(null);
            em.getTransaction().commit();
 
            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(idResumen);
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(idResumen);
            List<Imagen> imagenes = imagenDAO.listarPorResumen(idResumen);
            return Mappers.toDTO(resumen, dispositivos, trabajos, imagenes);
        } catch (IllegalArgumentException | AutorizacionException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al guardar la reseña: " + e.getMessage());
        } finally {
            em.close();
        }
    }
 
    /**
     * Sube una foto (antes/después) y la asocia a un ticket. Solo la usa
     * ResumenServlet cuando quien pide la subida es un ADMINISTRADOR (ver
     * plan de trabajo: "Apartado de fotos ... Modo administrador"); las
     * fotos ya subidas, en cambio, se muestran a ambos roles.
     */
    public ImagenDTO subirImagen(Long idResumen, String dataBase64, String descripcion, ImagenDTO.TipoImagen tipo)
            throws PersistenciaException {
        if (dataBase64 == null || dataBase64.isBlank()) {
            throw new IllegalArgumentException("dataBase64 es obligatorio");
        }
        if (!DATA_URL_IMAGE.matcher(dataBase64).matches()) {
            throw new IllegalArgumentException("Solo se permiten imágenes PNG, JPEG o WebP codificadas en Base64.");
        }
        int separator = dataBase64.indexOf(',');
        try {
            byte[] decoded = Base64.getDecoder().decode(dataBase64.substring(separator + 1));
            if (decoded.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido.");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La imagen enviada no es válida.");
        }
        String descripcionSegura = descripcion == null || descripcion.isBlank() ? null
                : Util.ValidationUtil.requiredText(descripcion, "descripcion", 500);
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, idResumen);
            if (resumen == null) {
                em.getTransaction().rollback();
                return null;
            }
            Imagen imagen = new Imagen();
            imagen.setResumen(resumen);
            imagen.setDataBase64(dataBase64);
            imagen.setDescripcion(descripcionSegura);
            imagen.setFechaSubida(LocalDateTime.now());
            if (tipo != null) {
                imagen.setTipo(Imagen.TipoImagen.valueOf(tipo.name()));
            }
            em.persist(imagen);
            em.getTransaction().commit();
            return Mappers.toDTO(imagen);
        } catch (IllegalArgumentException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al subir la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /** Elimina una imagen únicamente si pertenece al ticket indicado. */
    public boolean eliminarImagen(Long idResumen, Long idImagen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            int eliminadas = em.createQuery(
                    "DELETE FROM Imagen im WHERE im.id = :idImagen AND im.resumen.id = :idResumen")
                    .setParameter("idImagen", idImagen)
                    .setParameter("idResumen", idResumen)
                    .executeUpdate();
            em.getTransaction().commit();
            return eliminadas > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public boolean eliminarResumenCompleto(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            // Se borran primero los hijos (Dispositivo, Trabajo, Imagen,
            // Resumen_Dispositivos) para no violar las llaves foráneas hacia
            // Resumen.
            em.createQuery("DELETE FROM Dispositivo d WHERE d.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM Trabajo t WHERE t.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM Imagen im WHERE im.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM Resumen_Dispositivos rd WHERE rd.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            int filasAfectadas = em.createQuery("DELETE FROM Resumen r WHERE r.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.getTransaction().commit();
            return filasAfectadas > 0;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el ticket: " + e.getMessage());
        } finally {
            em.close();
        }
    }

}
