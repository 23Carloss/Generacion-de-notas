package Exceptions;

/** Signals a login failure without disclosing whether the account exists. */
public class CredencialesInvalidasException extends Exception {
    public CredencialesInvalidasException() {
        super("Credenciales incorrectas.");
    }
}
