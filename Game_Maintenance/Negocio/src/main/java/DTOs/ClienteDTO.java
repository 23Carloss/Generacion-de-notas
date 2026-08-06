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
    public enum ROL {
        ADMINISTRADOR, USUARIO
    }

    private Long id;
    private String nombre;
    private String telefono;
    private String correo;
    private ROL rol;
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

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public ROL getRol() {
        return rol;
    }

    public void setRol(ROL rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "ClienteDTO{" + "id=" + id + ", nombre=" + nombre + ", telefono=" + telefono + ", correo=" + correo + ", rol=" + rol + ", listaResumenes=" + listaResumenes + '}';
    }
    
}
