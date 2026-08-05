/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DTOs;

import java.util.List;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ClienteDTO {

    private Long id;
    private String nombre;
    private String telefono;
    private List<ResumenDTO> listaResumenes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public List<ResumenDTO> getListaResumenes() {
        return listaResumenes;
    }

    public void setListaResumenes(List<ResumenDTO> listaResumenes) {
        this.listaResumenes = listaResumenes;
    }
    
}
