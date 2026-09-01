/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class PasswordUtil {
    private static final int BCRYPT_COST = 12;
    private static final int MAX_PASSWORD_BYTES = 72;
 
    private PasswordUtil() {
    }
 
    public static String hash(String password) {
        validarLongitud(password);
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
    }
 
    /**
     * Compara una contraseña en texto plano (la que manda el usuario al
     * hacer login) contra el salt+hash guardados en la base de datos.
     */
    public static boolean verificar(String passwordPlano, String hashGuardado) {
        if (passwordPlano == null || hashGuardado == null || !hashGuardado.startsWith("$2")) {
            return false;
        }
        try {
            validarLongitud(passwordPlano);
            return BCrypt.checkpw(passwordPlano, hashGuardado);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifica únicamente cuentas heredadas de la versión que usaba
     * SHA-256(salt + contraseña). Al validarse correctamente, AuthService la
     * reemplaza de inmediato por un hash BCrypt.
     */
    public static boolean verificarLegado(String passwordPlano, String saltBase64, String hashGuardado) {
        if (passwordPlano == null || saltBase64 == null || hashGuardado == null
                || hashGuardado.startsWith("$2")) {
            return false;
        }
        try {
            validarLongitud(passwordPlano);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(saltBase64));
            byte[] calculated = digest.digest(passwordPlano.getBytes(StandardCharsets.UTF_8));
            byte[] stored = Base64.getDecoder().decode(hashGuardado);
            return MessageDigest.isEqual(calculated, stored);
        } catch (IllegalArgumentException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    public static void validarLongitud(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("La contraseña es demasiado larga.");
        }
    }
}
