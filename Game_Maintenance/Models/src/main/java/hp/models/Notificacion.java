package hp.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/** Notificación interna y persistente destinada a un usuario concreto. */
@Entity
@Table(name = "Notificacion", indexes = {
    @Index(name = "idx_notificacion_destinatario_fecha", columnList = "idDestinatario,fechaCreacion"),
    @Index(name = "idx_notificacion_destinatario_leida", columnList = "idDestinatario,leida")
})
public class Notificacion implements Serializable {

    public enum TipoNotificacion {
        USUARIO_REGISTRADO,
        ESTADO_TICKET_CAMBIADO,
        RESENA_CREADA,
        IMAGEN_TICKET_AGREGADA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idNotificacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idDestinatario", nullable = false)
    private Cliente destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(name = "titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "mensaje", nullable = false, length = 1000)
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fechaCreacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idResumen")
    private Resumen resumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idImagen")
    private Imagen imagen;

    @Column(name = "idClienteRelacionado")
    private Long clienteRelacionadoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cliente getDestinatario() { return destinatario; }
    public void setDestinatario(Cliente destinatario) { this.destinatario = destinatario; }
    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Resumen getResumen() { return resumen; }
    public void setResumen(Resumen resumen) { this.resumen = resumen; }
    public Imagen getImagen() { return imagen; }
    public void setImagen(Imagen imagen) { this.imagen = imagen; }
    public Long getClienteRelacionadoId() { return clienteRelacionadoId; }
    public void setClienteRelacionadoId(Long clienteRelacionadoId) { this.clienteRelacionadoId = clienteRelacionadoId; }
}
