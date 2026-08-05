/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Resumen;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ResumenDAO implements IGenericoDAO<Resumen, Long> {

    @Override
    public void insertar(Resumen resumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(resumen);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar el resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Resumen resumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(resumen);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen resumen = em.find(Resumen.class, id);
            if (resumen != null) {
                em.remove(resumen);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Resumen buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Resumen.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Resumen> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT r FROM Resumen r", Resumen.class).getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los resúmenes: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista los resúmenes (tickets) asociados a un cliente en particular.
     */
    public List<Resumen> listarPorCliente(Long idCliente) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT r FROM Resumen r WHERE r.cliente.id = :idCliente", Resumen.class)
                    .setParameter("idCliente", idCliente)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los resúmenes por cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
