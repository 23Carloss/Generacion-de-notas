package Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RegistrationRateLimiterTest {

    @Test
    void sixthRegistrationFromTheSameIpIsLimited() {
        String key = "test-registration-limit-" + System.nanoTime();

        for (int attempt = 0; attempt < 5; attempt++) {
            assertEquals(0, RegistrationRateLimiter.tryAcquire(key));
        }
        assertTrue(RegistrationRateLimiter.tryAcquire(key) > 0);
    }
}
