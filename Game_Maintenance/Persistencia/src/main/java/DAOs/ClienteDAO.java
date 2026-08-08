/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Cliente;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ClienteDAO implements IGenericoDAO<Cliente, Long> {

    @Override
    public void insertar(Cliente cliente) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar el cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Cliente cliente) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Cliente cliente = em.find(Cliente.class, id);
            if (cliente != null) {
                em.remove(cliente);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Cliente buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Cliente.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el cliente: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Cliente> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT c FROM Cliente c", Cliente.class).getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los clientes: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Busca un cliente a partir de su número telefónico.
     */
    public Cliente buscarPorTelefono(String telefono) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Cliente> resultado = em.createQuery(
                    "SELECT c FROM Cliente c WHERE c.telefono = :telefono", Cliente.class)
                    .setParameter("telefono", telefono)
                    .getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el cliente por teléfono: " + e.getMessage());
        } finally {
            em.close();
        }
    }
    public Cliente buscarPorCorreo(String correo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Cliente> resultado = em.createQuery(
                    "SELECT c FROM Cliente c WHERE c.correo = :correo", Cliente.class)
                    .setParameter("correo", correo)
                    .getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el cliente por correo: " + e.getMessage());
        } finally {
            em.close();
        }
    }

}
