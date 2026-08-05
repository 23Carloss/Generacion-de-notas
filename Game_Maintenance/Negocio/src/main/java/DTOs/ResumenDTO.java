/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DTOs;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ResumenDTO {
//    public enum ESTADO{
//        EnReparacion, Recibido, EnDiagnostico, ListoParaEntrega, Entregado;
//    }
    private Long id;
    private ClienteDTO cliente;    
    private List<DispositivoDTO> listaDispositivos;     
    private String comentariosCliente;
    private String descripcionProblema;
    private List<TrabajoDTO> listaTrabajos;
    private String estado;
    private String fechaCreacion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ClienteDTO getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDTO cliente) {
        this.cliente = cliente;
    }

    public List<DispositivoDTO> getListaDispositivos() {
        return listaDispositivos;
    }

    public void setListaDispositivos(List<DispositivoDTO> listaDispositivos) {
        this.listaDispositivos = listaDispositivos;
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

    public List<TrabajoDTO> getListaTrabajos() {
        return listaTrabajos;
    }

    public void setListaTrabajos(List<TrabajoDTO> listaTrabajos) {
        this.listaTrabajos = listaTrabajos;
    }
    
    public String getEstado() {
        return this.estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "ResumenDTO{" + "id=" + id + ", cliente=" + cliente + ", listaDispositivos=" + listaDispositivos + ", comentariosCliente=" + comentariosCliente + ", descripcionProblema=" + descripcionProblema + ", listaTrabajos=" + listaTrabajos + ", estado=" + estado + ", fechaCreacion=" + fechaCreacion + '}';
    }
    

    
}
