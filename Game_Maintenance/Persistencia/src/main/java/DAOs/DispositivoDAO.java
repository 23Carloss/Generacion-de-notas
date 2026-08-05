/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Dispositivo;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class DispositivoDAO implements IGenericoDAO<Dispositivo, Long> {

    @Override
    public void insertar(Dispositivo dispositivo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(dispositivo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar el dispositivo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Dispositivo dispositivo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(dispositivo);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el dispositivo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Dispositivo dispositivo = em.find(Dispositivo.class, id);
            if (dispositivo != null) {
                em.remove(dispositivo);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el dispositivo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Dispositivo buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Dispositivo.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el dispositivo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Dispositivo> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT d FROM Dispositivo d", Dispositivo.class).getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista los dispositivos asociados a un resumen (ticket) en particular.
     */
    public List<Dispositivo> listarPorResumen(Long idResumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT d FROM Dispositivo d WHERE d.resumen.id = :idResumen", Dispositivo.class)
                    .setParameter("idResumen", idResumen)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los dispositivos por resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista los dispositivos filtrados por plataforma (XBOX, PLAYSTATION, PC, LAPTOP).
     */
    public List<Dispositivo> listarPorPlataforma(Dispositivo.Plataforma plataforma) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT d FROM Dispositivo d WHERE d.plataforma = :plataforma", Dispositivo.class)
                    .setParameter("plataforma", plataforma)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los dispositivos por plataforma: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
