/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Resumen_Dispositivos;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class Resumen_DispositivosDAO implements IGenericoDAO<Resumen_Dispositivos, Long> {

    @Override
    public void insertar(Resumen_Dispositivos resumenDispositivos) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(resumenDispositivos);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar el resumen de dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Resumen_Dispositivos resumenDispositivos) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(resumenDispositivos);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar el resumen de dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Resumen_Dispositivos resumenDispositivos = em.find(Resumen_Dispositivos.class, id);
            if (resumenDispositivos != null) {
                em.remove(resumenDispositivos);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar el resumen de dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Resumen_Dispositivos buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Resumen_Dispositivos.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar el resumen de dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Resumen_Dispositivos> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT rd FROM Resumen_Dispositivos rd", Resumen_Dispositivos.class)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar los resúmenes de dispositivos: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Busca la relación resumen-dispositivo a partir del id del resumen (ticket).
     */
    public Resumen_Dispositivos buscarPorResumen(Long idResumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            List<Resumen_Dispositivos> resultado = em.createQuery(
                    "SELECT rd FROM Resumen_Dispositivos rd WHERE rd.resumen.id = :idResumen",
                    Resumen_Dispositivos.class)
                    .setParameter("idResumen", idResumen)
                    .getResultList();
            return resultado.isEmpty() ? null : resultado.get(0);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar por resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista todas las relaciones asociadas a un dispositivo en particular.
     */
    public List<Resumen_Dispositivos> listarPorDispositivo(Long idDispositivo) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT rd FROM Resumen_Dispositivos rd WHERE rd.dispositivo.id = :idDispositivo",
                    Resumen_Dispositivos.class)
                    .setParameter("idDispositivo", idDispositivo)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar por dispositivo: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
