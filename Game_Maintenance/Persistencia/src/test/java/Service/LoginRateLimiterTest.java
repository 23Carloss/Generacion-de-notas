package Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void fifthFailedAttemptTemporarilyBlocksTheAccountAndIpPair() {
        String key = "test-rate-limit-" + System.nanoTime();

        for (int attempt = 0; attempt < 4; attempt++) {
            assertEquals(0, LoginRateLimiter.registerFailure(key));
        }
        assertTrue(LoginRateLimiter.registerFailure(key) > 0);
        assertTrue(LoginRateLimiter.retryAfterSeconds(key) > 0);

        LoginRateLimiter.registerSuccess(key);
        assertEquals(0, LoginRateLimiter.retryAfterSeconds(key));
    }
}
