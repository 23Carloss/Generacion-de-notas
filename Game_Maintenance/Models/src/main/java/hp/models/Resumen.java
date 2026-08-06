/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

import hp.models.Cliente;
import hp.models.Dispositivo;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
@Entity
@Table(name="Resumen")
public class Resumen implements Serializable {
    public enum ESTADO{
        EnReparacion, Recibido, EnDiagnostico, ListoParaEntrega, Entregado;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="idResumen", nullable = true)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "idCliente", nullable = true)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "Estado")
    private ESTADO estado;
    
    @Column(name = "fechaCreacion")
    private LocalDateTime fechaCreacion;
    
    @OneToMany(mappedBy = "resumen")
    private List<Dispositivo> listaDispositivos; 
    
//    la lista de dispositivos la obtendremos de una consulta utilizando el idResumen en la tabla Resumen_Dispositivos
    
    @Column(name="comentariosCliente")
    private String comentariosCliente;
    
    @Column(name="descripcionProblema")
    private String descripcionProblema;
    
    @OneToMany(mappedBy = "resumen")
    private List<Trabajo> listaTrabajos;
    
    @Column(name= "resenaComentario")
    private String resenaComentario;
   
    @Column(name="calificacion")
    private Integer calificacion;

    public Resumen() {
    }

    public Long getId() {
        return id;
    }

    public ESTADO getEstado() {
        return estado;
    }

    public void setEstado(ESTADO estado) {
        this.estado = estado;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public List<Dispositivo> getListaDispositivos() {
        return listaDispositivos;
    }

    public void setListaDispositivos(List<Dispositivo> listaDispositivos) {
        this.listaDispositivos = listaDispositivos;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public String getResenaComentario() {
        return resenaComentario;
    }

    public void setResenaComentario(String resenaComentario) {
        this.resenaComentario = resenaComentario;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    @Override
    public String toString() {
        return "Resumen{" + "id=" + id + ", cliente=" + cliente + ", estado=" + estado + ", fechaCreacion=" + fechaCreacion + ", listaDispositivos=" + listaDispositivos + ", comentariosCliente=" + comentariosCliente + ", descripcionProblema=" + descripcionProblema + ", listaTrabajos=" + listaTrabajos + ", resenaComentario=" + resenaComentario + ", calificacion=" + calificacion + '}';
    }

}
