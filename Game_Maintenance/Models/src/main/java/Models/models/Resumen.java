/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
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

    @Column(name = "Estado")
    private ESTADO estado;
    
    @OneToMany(mappedBy = "resumen")
    private List<Dispositivo> listaDispositivos; 
    
//    la lista de dispositivos la obtendremos de una consulta utilizando el idResumen en la tabla Resumen_Dispositivos
    
    @Column(name="comentariosCliente")
    private String comentariosCliente;
    
    @Column(name="descripcionProblema")
    private String descripcionProblema;
    
    @OneToMany(mappedBy = "resumen")
    private List<Trabajo> listaTrabajos;

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

    @Override
    public String toString() {
        return "Resumen{" + "id=" + id + ", cliente=" + cliente + ", estado=" + estado + ", listaDispositivos=" + listaDispositivos + ", comentariosCliente=" + comentariosCliente + ", descripcionProblema=" + descripcionProblema + ", listaTrabajos=" + listaTrabajos + '}';
    }



}
