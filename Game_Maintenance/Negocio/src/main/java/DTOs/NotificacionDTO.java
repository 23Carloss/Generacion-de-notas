package DTOs;

public class NotificacionDTO {
    private Long id;
    private String tipo;
    private String titulo;
    private String mensaje;
    private boolean leida;
    private String fechaCreacion;
    private Long ticketId;
    private Long imagenId;
    private Long clienteRelacionadoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getImagenId() { return imagenId; }
    public void setImagenId(Long imagenId) { this.imagenId = imagenId; }
    public Long getClienteRelacionadoId() { return clienteRelacionadoId; }
    public void setClienteRelacionadoId(Long clienteRelacionadoId) { this.clienteRelacionadoId = clienteRelacionadoId; }
}
