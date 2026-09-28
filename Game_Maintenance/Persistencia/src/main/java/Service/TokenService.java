/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Service;

import hp.models.Cliente;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class TokenService {
 private static final ConcurrentHashMap<String, SesionInfo> SESIONES = new ConcurrentHashMap<>();
    private static final int TOKEN_LENGTH_BYTES = 32;
 
    private TokenService() {
    }
 
    /**
     * Datos mínimos que necesitan los servlets para autorizar una petición:
     * quién es el usuario (id) y qué puede hacer (rol).
     */
    public static class SesionInfo {
        public final Long clienteId;
        public final Cliente.ROL rol;
        private volatile Instant expiraEn;

        public SesionInfo(Long clienteId, Cliente.ROL rol) {
            this.clienteId = clienteId;
            this.rol = rol;
            this.expiraEn = siguienteExpiracion();
        }

        private boolean vigenteYRenovar() {
            Instant now = Instant.now();
            if (!now.isBefore(expiraEn)) {
                return false;
            }
            expiraEn = siguienteExpiracion();
            return true;
        }
    }
 
    public static String emitirToken(Cliente cliente) {
        byte[] bytes = new byte[TOKEN_LENGTH_BYTES];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        SESIONES.put(token, new SesionInfo(cliente.getId(), cliente.getRol()));
        return token;
    }
 
    
    public static SesionInfo validar(String token) {
        if (token == null) {
            return null;
        }
        SesionInfo sesion = SESIONES.get(token);
        if (sesion == null) {
            return null;
        }
        if (!sesion.vigenteYRenovar()) {
            SESIONES.remove(token, sesion);
            return null;
        }
        return sesion;
    }
 
    public static void invalidar(String token) {
        if (token != null) {
            SESIONES.remove(token);
        }
    }

    /** Cierra todas las sesiones activas de una cuenta eliminada. */
    public static void invalidarCliente(Long clienteId) {
        if (clienteId != null) {
            SESIONES.entrySet().removeIf(entry -> clienteId.equals(entry.getValue().clienteId));
        }
    }

    private static Instant siguienteExpiracion() {
        return Instant.now().plusSeconds(Util.AppConfig.sessionIdleTimeoutSeconds());
    }
}
