package Util;

import java.util.regex.Pattern;

/** Server-side validation for untrusted API input. */
public final class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+() .-]{7,24}$");

    private ValidationUtil() {
    }

    public static String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(field + " excede la longitud permitida.");
        }
        return trimmed;
    }

    public static String email(String value) {
        String normalized = requiredText(value, "correo", 254).toLowerCase();
        if (!EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("correo no tiene un formato válido.");
        }
        return normalized;
    }

    public static String phone(String value) {
        String normalized = requiredText(value, "telefono", 24);
        if (!PHONE.matcher(normalized).matches()) {
            throw new IllegalArgumentException("telefono no tiene un formato válido.");
        }
        return normalized;
    }
}
