package Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Limits public account creation per source IP to reduce automated abuse. */
public final class RegistrationRateLimiter {

    private static final int MAX_REGISTRATIONS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final ConcurrentHashMap<String, Window> WINDOWS = new ConcurrentHashMap<>();

    private RegistrationRateLimiter() {
    }

    public static long tryAcquire(String sourceIp) {
        Instant now = Instant.now();
        long[] retryAfter = {0};
        WINDOWS.compute(sourceIp, (ignored, current) -> {
            if (current == null || current.startedAt.plus(WINDOW).isBefore(now)) {
                current = new Window(now);
            }
            if (current.registrations >= MAX_REGISTRATIONS) {
                retryAfter[0] = Math.max(1, Duration.between(now, current.startedAt.plus(WINDOW)).toSeconds());
            } else {
                current.registrations++;
            }
            return current;
        });
        return retryAfter[0];
    }

    private static final class Window {
        private final Instant startedAt;
        private int registrations;

        private Window(Instant startedAt) {
            this.startedAt = startedAt;
        }
    }
}
