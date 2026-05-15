package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.web.advice.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic brute-force protection for the login endpoint: after a number of consecutive failures for
 * an email the account is locked out for a cooldown. State is kept in memory, which is enough for a
 * single instance; a multi-instance deployment would back this with a shared store and also key on
 * client IP.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockout;
    private final Clock clock;
    private final Map<String, Window> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.security.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.security.login.lockout:PT15M}") Duration lockout,
                               Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockout = lockout;
        this.clock = clock;
    }

    public void assertNotLocked(String email) {
        Window window = attempts.get(key(email));
        if (window != null && window.lockedUntil != null && window.lockedUntil.isAfter(clock.instant())) {
            throw ApiException.tooManyRequests("Too many failed sign-in attempts. Please try again later.");
        }
    }

    public void recordFailure(String email) {
        Instant now = clock.instant();
        attempts.compute(key(email), (k, window) -> {
            if (window == null || window.isExpired(now, lockout)) {
                window = new Window(now);
            }
            window.failures++;
            if (window.failures >= maxAttempts) {
                window.lockedUntil = now.plus(lockout);
            }
            return window;
        });
    }

    public void recordSuccess(String email) {
        attempts.remove(key(email));
    }

    private static String key(String email) {
        return email == null ? "" : email.toLowerCase(Locale.ROOT);
    }

    private static final class Window {
        private final Instant firstFailureAt;
        private int failures;
        private Instant lockedUntil;

        private Window(Instant firstFailureAt) {
            this.firstFailureAt = firstFailureAt;
        }

        private boolean isExpired(Instant now, Duration lockout) {
            Instant resetAt = lockedUntil != null ? lockedUntil : firstFailureAt.plus(lockout);
            return !resetAt.isAfter(now);
        }
    }
}
