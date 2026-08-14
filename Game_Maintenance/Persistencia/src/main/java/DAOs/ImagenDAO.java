/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import ConexionDB.ManejadorConexiones;
import hp.models.Imagen;
import java.util.List;
import javax.persistence.EntityManager;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ImagenDAO implements IGenericoDAO<Imagen, Long> {

    @Override
    public void insertar(Imagen imagen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(imagen);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al insertar la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void actualizar(Imagen imagen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(imagen);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al actualizar la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public void eliminar(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            Imagen imagen = em.find(Imagen.class, id);
            if (imagen != null) {
                em.remove(imagen);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw new PersistenciaException("Error al eliminar la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public Imagen buscarPorId(Long id) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.find(Imagen.class, id);
        } catch (Exception e) {
            throw new PersistenciaException("Error al buscar la imagen: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    @Override
    public List<Imagen> listarTodos() throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery("SELECT im FROM Imagen im", Imagen.class).getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar las imágenes: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    /**
     * Lista las imágenes (fotos de antes/después) asociadas a un resumen
     * (ticket) en particular, de la más reciente a la más antigua, para que
     * funcionen como un pequeño "log" de subida.
     */
    public List<Imagen> listarPorResumen(Long idResumen) throws PersistenciaException {
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT im FROM Imagen im WHERE im.resumen.id = :idResumen ORDER BY im.fechaSubida DESC",
                    Imagen.class)
                    .setParameter("idResumen", idResumen)
                    .getResultList();
        } catch (Exception e) {
            throw new PersistenciaException("Error al listar las imágenes por resumen: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
