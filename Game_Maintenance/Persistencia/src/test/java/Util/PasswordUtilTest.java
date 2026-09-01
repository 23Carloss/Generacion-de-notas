package Util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PasswordUtilTest {

    @Test
    void bcryptHashVerifiesOnlyTheOriginalPassword() {
        String hash = PasswordUtil.hash("correct horse battery staple");

        assertTrue(hash.startsWith("$2"));
        assertTrue(PasswordUtil.verificar("correct horse battery staple", hash));
        assertFalse(PasswordUtil.verificar("incorrecta", hash));
    }

    @Test
    void bcryptPasswordIsNeverSilentlyTruncated() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hash("a".repeat(73)));
    }

    @Test
    void validatesLegacyHashOnlyForMigration() throws Exception {
        String password = "correct horse battery staple";
        String salt = Base64.getEncoder().encodeToString("salt-de-prueba-1".getBytes(StandardCharsets.UTF_8));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Base64.getDecoder().decode(salt));
        String legacyHash = Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));

        assertTrue(PasswordUtil.verificarLegado(password, salt, legacyHash));
        assertFalse(PasswordUtil.verificarLegado("incorrecta", salt, legacyHash));
        assertFalse(PasswordUtil.verificarLegado(password, salt, PasswordUtil.hash(password)));
    }
}
