/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package Service;

import ConexionDB.ManejadorConexiones;
import DAOs.ClienteDAO;
import DTOs.ClienteDTO;
import Exceptions.PersistenciaException;
import Exceptions.CredencialesInvalidasException;
import Util.PasswordUtil;
import Util.ValidationUtil;
import hp.models.Cliente;
import javax.persistence.EntityManager;

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
 
    public LoginResult login(String correo, String password) throws PersistenciaException, CredencialesInvalidasException {
        if (correo == null || correo.isBlank() || password == null || password.isBlank()) {
            throw new CredencialesInvalidasException();
        }

        Cliente cliente = clienteDAO.buscarPorCorreo(normalizarCorreo(correo));
        boolean credencialesValidas = cliente != null
                && PasswordUtil.verificar(password, cliente.getPasswordHash());

        if (!credencialesValidas && cliente != null
                && PasswordUtil.verificarLegado(password, cliente.getPasswordSalt(), cliente.getPasswordHash())) {
            // Las cuentas existentes conservaban salt y SHA-256. Una vez que
            // demuestran conocer su contraseña, se actualizan a BCrypt.
            cliente.setPasswordHash(PasswordUtil.hash(password));
            cliente.setPasswordSalt(null);
            clienteDAO.actualizar(cliente);
            credencialesValidas = true;
        }

        if (!credencialesValidas) {
            throw new CredencialesInvalidasException();
        }
 
        String token = TokenService.emitirToken(cliente);
        return new LoginResult(token, Mappers.toDTO(cliente));
    }
 
    public LoginResult registrar(String nombre, String telefono, String correo, String password)
            throws PersistenciaException {
        String nombreSeguro = ValidationUtil.requiredText(nombre, "nombre", 100);
        String telefonoSeguro = ValidationUtil.phone(telefono);
        String correoNormalizado = ValidationUtil.email(correo);
        if (password.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }
        PasswordUtil.validarLongitud(password);
 
        if (clienteDAO.buscarPorCorreo(correoNormalizado) != null) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo");
        }
 
        String hash = PasswordUtil.hash(password);
 
        Cliente cliente = new Cliente();
        cliente.setNombre(nombreSeguro);
        cliente.setTelefono(telefonoSeguro);
        cliente.setCorreo(correoNormalizado);
        cliente.setPasswordHash(hash);
        // El registro público siempre crea usuarios comunes; el rol
        // ADMINISTRADOR se asigna manualmente en la base de datos (ver
        // reporte de la Etapa 1).
        cliente.setRol(Cliente.ROL.USUARIO);
 
        EntityManager em = ManejadorConexiones.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(cliente);
            em.flush();
            NotificacionService.notificarRegistro(em, cliente);
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new PersistenciaException("Error al registrar el cliente: " + e.getMessage());
        } finally {
            em.close();
        }
 
        // Por comodidad, registrarse también deja la sesión iniciada (evita
        // pedirle login inmediatamente después de crear la cuenta).
        String token = TokenService.emitirToken(cliente);
        return new LoginResult(token, Mappers.toDTO(cliente));
    }
 
    private String normalizarCorreo(String correo) {
        try {
            return ValidationUtil.email(correo);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
