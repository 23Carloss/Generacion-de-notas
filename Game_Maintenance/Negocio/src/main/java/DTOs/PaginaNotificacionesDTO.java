package DTOs;

import java.util.List;

public class PaginaNotificacionesDTO {
    private List<NotificacionDTO> notificaciones;
    private int pagina;
    private int tamano;
    private long total;
    private long noLeidas;

    public PaginaNotificacionesDTO(List<NotificacionDTO> notificaciones, int pagina, int tamano,
            long total, long noLeidas) {
        this.notificaciones = notificaciones;
        this.pagina = pagina;
        this.tamano = tamano;
        this.total = total;
        this.noLeidas = noLeidas;
    }

    public List<NotificacionDTO> getNotificaciones() { return notificaciones; }
    public int getPagina() { return pagina; }
    public int getTamano() { return tamano; }
    public long getTotal() { return total; }
    public long getNoLeidas() { return noLeidas; }
}
