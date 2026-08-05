package Service;

import Service.Mappers;
import DAOs.DispositivoDAO;
import DAOs.TrabajoDAO;
import DTOs.DispositivoDTO;
import DTOs.ResumenDTO;
import DTOs.TrabajoDTO;
import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Cliente;
import hp.models.Dispositivo;
import hp.models.Resumen;
import hp.models.Trabajo;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orquesta la creación/lectura/actualización/borrado de un ticket (Resumen)
 * junto con sus Dispositivos y Trabajos relacionados, en una sola transacción
 * cuando corresponde (los DAOs individuales usan una transacción por llamada,
 * lo cual no alcanza para crear/eliminar un ticket completo de forma atómica).
 */
public class ResumenService {

    private final DispositivoDAO dispositivoDAO = new DispositivoDAO();
    private final TrabajoDAO trabajoDAO = new TrabajoDAO();

    public List<ResumenDTO> listarResumenesDTO() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Resumen> resumenes = em.createQuery("SELECT r FROM Resumen r", Resumen.class).getResultList();
            resumenes.sort(Comparator.comparing(Resumen::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));

            List<ResumenDTO> dtos = new ArrayList<>();
            for (Resumen r : resumenes) {
                List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(r.getId());
                List<Trabajo> trabajos = trabajoDAO.listarPorResumen(r.getId());
                dtos.add(Mappers.toDTO(r, dispositivos, trabajos));
            }
            return dtos;
        } catch (PersistenciaException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los tickets: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public ResumenDTO obtenerResumenDTO(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            Resumen r = em.find(Resumen.class, id);
            if (r == null) return null;
            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(id);
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(id);
            return Mappers.toDTO(r, dispositivos, trabajos);
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

            Resumen resumen = new Resumen();
            resumen.setCliente(cliente);
            resumen.setDescripcionProblema(entrada.getDescripcionProblema());
            resumen.setComentariosCliente(entrada.getComentariosCliente());
            resumen.setEstado(Resumen.ESTADO.Recibido);
            resumen.setFechaCreacion(LocalDateTime.now());
            em.persist(resumen);

            List<Dispositivo> dispositivosCreados = new ArrayList<>();
            if (entrada.getListaDispositivos() != null) {
                for (DispositivoDTO d : entrada.getListaDispositivos()) {
                    Dispositivo dispositivo = new Dispositivo();
                    dispositivo.setModeloDispositivo(d.getModeloDispositivo());
                    dispositivo.setDetallesDispositivo(d.getDetallesDispositivo());
                    if (d.getPlataforma() != null) {
                        dispositivo.setPlataforma(Dispositivo.Plataforma.valueOf(d.getPlataforma().name()));
                    }
                    dispositivo.setResumen(resumen);
                    em.persist(dispositivo);
                    dispositivosCreados.add(dispositivo);
                }
            }

            List<Trabajo> trabajosCreados = new ArrayList<>();
            if (entrada.getListaTrabajos() != null) {
                for (TrabajoDTO t : entrada.getListaTrabajos()) {
                    Trabajo trabajo = new Trabajo();
                    if (t.getTipoTrabajo() != null) {
                        trabajo.setTipoTrabajo(Trabajo.TipoTrabajo.valueOf(t.getTipoTrabajo().name()));
                    }
                    trabajo.setPrecio(t.getPrecio());
                    trabajo.setResumen(resumen);
                    em.persist(trabajo);
                    trabajosCreados.add(trabajo);
                }
            }

            em.getTransaction().commit();
            return Mappers.toDTO(resumen, dispositivosCreados, trabajosCreados);
        } catch (IllegalArgumentException e) {
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

    public ResumenDTO actualizarEstado(Long id, String estadoFrontend) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, id);
            if (resumen == null) {
                em.getTransaction().rollback();
                return null;
            }
            resumen.setEstado(Mappers.estadoDesdeFrontend(estadoFrontend));
            em.getTransaction().commit();

            List<Dispositivo> dispositivos = dispositivoDAO.listarPorResumen(id);
            List<Trabajo> trabajos = trabajoDAO.listarPorResumen(id);
            return Mappers.toDTO(resumen, dispositivos, trabajos);
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

    public boolean eliminarResumenCompleto(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            // Se borran primero los hijos (Dispositivo, Trabajo, Resumen_Dispositivos)
            // para no violar las llaves foráneas hacia Resumen.
            em.createQuery("DELETE FROM Dispositivo d WHERE d.resumen.id = :id")
                    .setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM Trabajo t WHERE t.resumen.id = :id")
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
