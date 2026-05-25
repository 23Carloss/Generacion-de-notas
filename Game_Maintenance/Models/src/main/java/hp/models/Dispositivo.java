/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class Dispositivo {
    public enum Plataforma {
    XBOX, PLAYSTATION, PC, LAPTOP
    }
    private Long id; 
    private String detallesDispositivo, modeloDispositivo;
    private Long idCliente;
    private Plataforma plataforma;

    public Dispositivo() {
    }

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

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    @Override
    public String toString() {
        return "Dispositivo{" + "id=" + id + ", detallesDispositivo=" + detallesDispositivo + ", modeloDispositivo=" + modeloDispositivo + ", idCliente=" + idCliente + ", plataforma=" + plataforma + '}';
    }
    
    

}
