/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DTOs;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class DispositivoDTO {
    public enum Plataforma {
    XBOX, PLAYSTATION, PC, LAPTOP
    }
    private Long id; 
    
    private String detallesDispositivo;
    
    private String modeloDispositivo;
    private Plataforma plataforma;
    private ResumenDTO resumen;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDetallesDispositivo() {
        return detallesDispositivo;
    }

    public void setDetallesDispositivo(String detallesDispositivo) {
        this.detallesDispositivo = detallesDispositivo;
    }

    public String getModeloDispositivo() {
        return modeloDispositivo;
    }

    public void setModeloDispositivo(String modeloDispositivo) {
        this.modeloDispositivo = modeloDispositivo;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    public ResumenDTO getResumen() {
        return resumen;
    }

    public void setResumen(ResumenDTO resumen) {
        this.resumen = resumen;
    }
    

}
