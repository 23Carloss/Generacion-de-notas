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
@Table(name="Dispositivo")
public class Dispositivo implements Serializable {
    public enum Plataforma {
    XBOX, PLAYSTATION, PC, LAPTOP
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDispositivo", nullable = true)
    private Long id; 
    
    @Column(name="detallesDispositivo")
    private String detallesDispositivo;
    
    @Column(name="modeloDispositivo")
    private String modeloDispositivo;
    
//    @OneToOne
//    @JoinColumn(name = "idUsuario", nullable = true)
//    private Cliente cliente;
    
    @Column(name="plataforma")
    @Enumerated(EnumType.STRING)
    private Plataforma plataforma;
    
    @ManyToOne
    @JoinColumn(name = "idResumen")
    private Resumen resumen;

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

    public Plataforma getPlataforma() {
        return plataforma;
    }

    public void setPlataforma(Plataforma plataforma) {
        this.plataforma = plataforma;
    }

    public Resumen getResumen() {
        return resumen;
    }

    public void setResumen(Resumen resumen) {
        this.resumen = resumen;
    }

    @Override
    public String toString() {
        return "Dispositivo{" + "id=" + id + ", detallesDispositivo=" + detallesDispositivo + ", modeloDispositivo=" + modeloDispositivo + ", plataforma=" + plataforma + ", resumen=" + resumen + '}';
    }
    
}
