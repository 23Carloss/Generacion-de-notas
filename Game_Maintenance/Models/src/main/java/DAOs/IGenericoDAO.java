/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */

package DAOs;

import Exceptions.PersistenciaException;
import java.util.List;

/**
 * Contrato genérico que deben cumplir todos los DAO del sistema.
 *
 * @param <T>  tipo de la entidad
 * @param <ID> tipo de la llave primaria de la entidad
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public interface IGenericoDAO<T, ID> {

    void insertar(T entidad) throws PersistenciaException;

    void actualizar(T entidad) throws PersistenciaException;

    void eliminar(ID id) throws PersistenciaException;

    T buscarPorId(ID id) throws PersistenciaException;

    List<T> listarTodos() throws PersistenciaException;
}
