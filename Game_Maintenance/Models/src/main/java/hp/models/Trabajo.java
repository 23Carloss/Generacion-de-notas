/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
@Entity
@Table(name="Trabajo")
public class Trabajo implements Serializable {
    public enum TipoTrabajo {
        REPARACION, DIAGNOSTICO, CHIPEO, INSTALACION_JUEGOS, MANTENIMIENTO
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTrabajo")
    private Long id;
    
    
    @Enumerated(EnumType.STRING)
    @Column(name="tipoTrabajo")
    private TipoTrabajo tipoTrabajo;
    
    @ManyToOne
    @JoinColumn(name = "idResumen")
    private Resumen resumen;
    
    @Column(name="precio")
    private Double precio;

    // Estos datos solo aplican cuando el trabajo es una REPARACION. El
    // precio total del trabajo se conserva en precio = unidades * precioUnitario.
    @Column(name="nombrePieza")
    private String nombrePieza;

    @Column(name="unidades")
    private Integer unidades;

    @Column(name="precioUnitario")
    private Double precioUnitario;

    public Trabajo() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoTrabajo getTipoTrabajo() {
        return tipoTrabajo;
    }

    public void setTipoTrabajo(TipoTrabajo tipoTrabajo) {
        this.tipoTrabajo = tipoTrabajo;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public String getNombrePieza() {
        return nombrePieza;
    }

    public void setNombrePieza(String nombrePieza) {
        this.nombrePieza = nombrePieza;
    }

    public Integer getUnidades() {
        return unidades;
    }

    public void setUnidades(Integer unidades) {
        this.unidades = unidades;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Resumen getResumen() {
        return resumen;
    }

    public void setResumen(Resumen resumen) {
        this.resumen = resumen;
    }

    @Override
    public String toString() {
        return "Trabajo{" + "id=" + id + ", tipoTrabajo=" + tipoTrabajo + ", resumen=" + resumen
                + ", precio=" + precio + ", nombrePieza=" + nombrePieza + ", unidades=" + unidades
                + ", precioUnitario=" + precioUnitario + '}';
    }
    

   
    

}
