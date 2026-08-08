/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Service;

import DAOs.ClienteDAO;
import DTOs.ClienteDTO;
import Exceptions.PersistenciaException;
import Util.PasswordUtil;
import hp.models.Cliente;

/**
 *
 * @author $Luis Carlos Manjarrez Gonzalez
 */
public class AuthService {
    private static final int PASSWORD_MIN_LENGTH = 8;
 
    private final ClienteDAO clienteDAO = new ClienteDAO();
 
    /**
     * Resultado de un login (o de un registro, que también deja la sesión
     * iniciada). Se expone como clase simple en vez de un Map para que el
     * servlet no tenga que acordarse de las claves "a mano".
     */
    public static class LoginResult {
        public final String token;
        public final ClienteDTO cliente;
 
        public LoginResult(String token, ClienteDTO cliente) {
            this.token = token;
            this.cliente = cliente;
        }
    }
 
    public LoginResult login(String correo, String password) throws PersistenciaException {
        if (correo == null || correo.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("correo y password son obligatorios");
        }
 
        Cliente cliente = clienteDAO.buscarPorCorreo(normalizarCorreo(correo));
        boolean credencialesValidas = cliente != null
                && PasswordUtil.verificar(password, cliente.getPasswordSalt(), cliente.getPasswordHash());
 
        if (!credencialesValidas) {
            // Mismo mensaje si el correo no existe o si la contraseña es
            // incorrecta, para no revelar qué correos están registrados.
            throw new IllegalArgumentException("Correo o contraseña incorrectos");
        }
 
        String token = TokenService.emitirToken(cliente);
        return new LoginResult(token, Mappers.toDTO(cliente));
    }
 
    public LoginResult registrar(String nombre, String telefono, String correo, String password)
            throws PersistenciaException {
        if (nombre == null || nombre.isBlank()
                || telefono == null || telefono.isBlank()
                || correo == null || correo.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("nombre, telefono, correo y password son obligatorios");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }
 
        String correoNormalizado = normalizarCorreo(correo);
        if (clienteDAO.buscarPorCorreo(correoNormalizado) != null) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
 
        String salt = PasswordUtil.generarSalt();
        String hash = PasswordUtil.hash(password, salt);
 
        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setTelefono(telefono.trim());
        cliente.setCorreo(correoNormalizado);
        cliente.setPasswordSalt(salt);
        cliente.setPasswordHash(hash);
        // El registro público siempre crea usuarios comunes; el rol
        // ADMINISTRADOR se asigna manualmente en la base de datos (ver
        // reporte de la Etapa 1).
        cliente.setRol(Cliente.ROL.USUARIO);
 
        clienteDAO.insertar(cliente);
 
        // Por comodidad, registrarse también deja la sesión iniciada (evita
        // pedirle login inmediatamente después de crear la cuenta).
        String token = TokenService.emitirToken(cliente);
        return new LoginResult(token, Mappers.toDTO(cliente));
    }
 
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }
}
