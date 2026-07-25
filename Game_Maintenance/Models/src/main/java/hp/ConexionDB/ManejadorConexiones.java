/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.ConexionDB;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ManejadorConexiones {
        private static EntityManagerFactory emFactory =
            Persistence.createEntityManagerFactory(
                    "TicketsPU");

    public static EntityManager getEntityManager() {
        return emFactory.createEntityManager();
    }
    
    //metodos para evitar multiples conexiones por los servlets
    public static void Inicializar(){
        if(emFactory == null){
            emFactory = Persistence.createEntityManagerFactory(
                    "TicketsPU");
        }
    }
    
    public static void cerrar(){
        if(emFactory!= null && emFactory.isOpen()){
            emFactory.close();
                    
        }
    }
        

    // MAIN PARA MAPEAR LAS TABLAS EN MYSQL
    public static void main(String[] args) {
        System.out.println("Iniciando JPA...");
        EntityManager em = ManejadorConexiones.getEntityManager();

        //Iniciamos una transacción para que se creen las tablas en la base de datos
        em.getTransaction().begin();
        em.getTransaction().commit();
        //Y cerramos el EntityManager

        em.close();
        // Se cierra pa

        System.out.println("Base construida correctamente, eso es todo");
    }

}
