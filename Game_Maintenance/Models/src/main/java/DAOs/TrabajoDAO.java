/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import hp.ConexionDB.ManejadorConexiones;
import hp.models.Trabajo;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class TrabajoDAO implements IGenericoDAO<Trabajo, Long> {

    @Override
    public void insertar(Trabajo trabajo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(trabajo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar el trabajo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Trabajo trabajo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(trabajo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el trabajo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Trabajo trabajo = em.find(Trabajo.class, id);
            if (trabajo != null) {
                em.remove(trabajo);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el trabajo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Trabajo buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Trabajo.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el trabajo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Trabajo> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Trabajo t", Trabajo.class).getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los trabajos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista los trabajos asociados a un resumen (ticket) en particular.
     */
    public List<Trabajo> listarPorResumen(Long idResumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT t FROM Trabajo t WHERE t.resumen.id = :idResumen", Trabajo.class)
                    .setParameter("idResumen", idResumen)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los trabajos por resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista los trabajos filtrados por tipo (REPARACION, DIAGNOSTICO, CHIPEO, INSTALACION_JUEGOS, MANTENIMIENTO).
     */
    public List<Trabajo> listarPorTipo(Trabajo.TipoTrabajo tipoTrabajo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT t FROM Trabajo t WHERE t.tipoTrabajo = :tipo", Trabajo.class)
                    .setParameter("tipo", tipoTrabajo)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los trabajos por tipo: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
