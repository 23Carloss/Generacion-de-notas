/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class PasswordUtil {
    private static final String ALGORITMO = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;
 
    private PasswordUtil() {
    }
 
    /**
     * Genera un salt aleatorio nuevo, codificado en Base64 para poder
     * guardarlo directo en una columna de texto.
     */
    public static String generarSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
 
    /**
     * Calcula el hash de una contraseña dado un salt (ambos en Base64 de
     * entrada/salida). Se usa tanto al registrar como al hacer login.
     */
    public static String hash(String password, String saltBase64) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITMO);
            digest.update(Base64.getDecoder().decode(saltBase64));
            byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            // SHA-256 y UTF-8 siempre están disponibles en cualquier JVM estándar;
            // si esto falla es un problema del entorno, no algo recuperable.
            throw new RuntimeException("No se pudo calcular el hash de la contraseña", e);
        }
    }
 
    /**
     * Compara una contraseña en texto plano (la que manda el usuario al
     * hacer login) contra el salt+hash guardados en la base de datos.
     */
    public static boolean verificar(String passwordPlano, String saltBase64, String hashGuardado) {
        if (passwordPlano == null || saltBase64 == null || hashGuardado == null) {
            return false;
        }
        String hashCalculado = hash(passwordPlano, saltBase64);
        return hashCalculado.equals(hashGuardado);
    }
}
