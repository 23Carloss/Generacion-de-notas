package Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory login abuse protection keyed by the requesting IP and account. */
public final class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);
    private static final ConcurrentHashMap<String, Attempt> ATTEMPTS = new ConcurrentHashMap<>();

    private LoginRateLimiter() {
    }

    public static long retryAfterSeconds(String key) {
        Attempt attempt = ATTEMPTS.get(key);
        if (attempt == null || attempt.blockedUntil == null) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), attempt.blockedUntil).toSeconds();
        if (seconds <= 0) {
            ATTEMPTS.remove(key, attempt);
            return 0;
        }
        return seconds;
    }

    public static long registerFailure(String key) {
        Instant now = Instant.now();
        Attempt attempt = ATTEMPTS.compute(key, (ignored, current) -> {
            if (current == null || current.firstFailure.plus(WINDOW).isBefore(now)) {
                current = new Attempt(now);
            }
            current.failures++;
            if (current.failures >= MAX_FAILURES) {
                current.blockedUntil = now.plus(BLOCK_DURATION);
            }
            return current;
        });
        return attempt.blockedUntil == null ? 0 : Duration.between(now, attempt.blockedUntil).toSeconds();
    }

    public static void registerSuccess(String key) {
        ATTEMPTS.remove(key);
    }

    private static final class Attempt {
        private final Instant firstFailure;
        private int failures;
        private Instant blockedUntil;

        private Attempt(Instant firstFailure) {
            this.firstFailure = firstFailure;
        }
    }
}
