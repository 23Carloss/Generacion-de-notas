/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package hp.models;

import java.io.Serializable;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
@Entity
@Table(name="Cliente")
public class Cliente implements Serializable{
    enum ROL { ADMINISTRADOR, USUARIO}
    
    @Id
    @Column(name = "idCliente")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name="nombre")
    private String nombre;
    
    @Column(name="telefono")
    private String telefono;
    
    @OneToMany(mappedBy = "cliente")
    private List<Resumen> listaResumenes;
    
    @Enumerated(EnumType.STRING)
    @Column(name="rol")
    private ROL rol;
    
    @Column(name="correo", unique = true)
    private String correo;
    
    @Column(name="passwordHash")
    private String paswordHash;
   
    @Column(name="passwordSalt")
    private String paswordSalt;
    
   public Cliente() {
   }
 
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

    public List<Resumen> getListaResumenes() {
        return listaResumenes;
    }

    public void setListaResumenes(List<Resumen> listaResumenes) {
        this.listaResumenes = listaResumenes;
    }

    public ROL getRol() {
        return rol;
    }

    public void setRol(ROL rol) {
        this.rol = rol;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPaswordHash() {
        return paswordHash;
    }

    public void setPaswordHash(String paswordHash) {
        this.paswordHash = paswordHash;
    }

    public String getPaswordSalt() {
        return paswordSalt;
    }

    public void setPaswordSalt(String paswordSalt) {
        this.paswordSalt = paswordSalt;
    }

    @Override
    public String toString() {
        return "Cliente{" + "id=" + id + ", nombre=" + nombre + ", telefono=" + telefono + ", listaResumenes=" + listaResumenes + ", rol=" + rol + ", correo=" + correo + '}';
    }

}
