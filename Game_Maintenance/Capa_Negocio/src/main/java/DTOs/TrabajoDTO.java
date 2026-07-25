/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DTOs;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class TrabajoDTO {

    public enum TipoTrabajo {
        REPARACION, DIAGNOSTICO, CHIPEO, INSTALACION_JUEGOS, MANTENIMIENTO
    }

    private Long id;
    
    private TipoTrabajo tipoTrabajo;
    
    private ResumenDTO resumen;
    
    private Double precio;

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

    public ResumenDTO getResumen() {
        return resumen;
    }

    public void setResumen(ResumenDTO resumen) {
        this.resumen = resumen;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }
    

}
