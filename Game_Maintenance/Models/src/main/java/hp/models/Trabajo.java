/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class Trabajo {
    public enum TipoTrabajo {
        REPARACION, DIAGNOSTICO, CHIPEO, INSTALACION_JUEGOS, MANTENIMIENTO
    }
    
    private Long id;
    private TipoTrabajo tipoTrabajo;
    private Double precio;

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

    @Override
    public String toString() {
        return "Trabajo{" + "id=" + id + ", tipoTrabajo=" + tipoTrabajo + ", precio=" + precio + '}';
    }
    

}
