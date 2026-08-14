/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package DTOs;

/**
 * DTO "plano" de hp.models.Imagen. dataBase64 se guarda y se manda tal cual
 * como Data URL completa (por ejemplo "data:image/png;base64,AAAA...")
 * generada por FileReader.readAsDataURL en el front, así el <img> del
 * navegador la puede usar directo como src sin que el backend tenga que
 * guardar el mime-type por separado.
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class ImagenDTO {

    public enum TipoImagen {
        ANTES, DESPUES
    }

    private Long id;
    private TipoImagen tipo;
    private String dataBase64;
    private String descripcion;
    private String fechaSubida;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoImagen getTipo() {
        return tipo;
    }

    public void setTipo(TipoImagen tipo) {
        this.tipo = tipo;
    }

    public String getDataBase64() {
        return dataBase64;
    }

    public void setDataBase64(String dataBase64) {
        this.dataBase64 = dataBase64;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(String fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    @Override
    public String toString() {
        return "ImagenDTO{" + "id=" + id + ", tipo=" + tipo + ", descripcion=" + descripcion
                + ", fechaSubida=" + fechaSubida + '}';
    }
}
