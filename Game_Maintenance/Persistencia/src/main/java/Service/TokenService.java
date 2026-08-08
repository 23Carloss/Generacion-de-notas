/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Service;

import hp.models.Cliente;
import java.security.SecureRandom;
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
 
        public SesionInfo(Long clienteId, Cliente.ROL rol) {
            this.clienteId = clienteId;
            this.rol = rol;
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
        return SESIONES.get(token);
    }
 
    public static void invalidar(String token) {
        if (token != null) {
            SESIONES.remove(token);
        }
    }
}
