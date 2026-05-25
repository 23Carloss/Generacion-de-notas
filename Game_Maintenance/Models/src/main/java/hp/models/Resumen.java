/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

import java.util.List;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class Resumen {
    private Long id , idCliente, idDispositivo;
    private String comentariosCliente, descripcionProblema; 
    private List<Trabajo> listaTrabajos;

    public Resumen() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Long getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(Long idDispositivo) {
        this.idDispositivo = idDispositivo;
    }

    public String getComentariosCliente() {
        return comentariosCliente;
    }

    public void setComentariosCliente(String comentariosCliente) {
        this.comentariosCliente = comentariosCliente;
    }

    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    public void setDescripcionProblema(String descripcionProblema) {
        this.descripcionProblema = descripcionProblema;
    }

    public List<Trabajo> getListaTrabajos() {
        return listaTrabajos;
    }

    public void setListaTrabajos(List<Trabajo> listaTrabajos) {
        this.listaTrabajos = listaTrabajos;
    }

    @Override
    public String toString() {
        return "Resumen{" + "id=" + id + ", idCliente=" + idCliente + ", idDispositivo=" + idDispositivo + ", comentariosCliente=" + comentariosCliente + ", descripcionProblema=" + descripcionProblema + ", listaTrabajos=" + listaTrabajos + '}';
    }
    

}
