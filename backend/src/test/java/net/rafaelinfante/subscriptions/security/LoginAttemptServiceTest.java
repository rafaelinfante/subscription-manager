package net.rafaelinfante.subscriptions.security;

import net.rafaelinfante.subscriptions.support.MutableClock;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private final MutableClock clock = MutableClock.at(Instant.parse("2026-06-25T10:00:00Z"));
    private final LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);

    @Test
    void locksOutAfterTheConfiguredNumberOfFailures() {
        service.recordFailure("user@example.com");
        service.recordFailure("user@example.com");
        assertThatCode(() -> service.assertNotLocked("user@example.com")).doesNotThrowAnyException();

        service.recordFailure("user@example.com");
        assertThatThrownBy(() -> service.assertNotLocked("user@example.com"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Too many failed");
    }

    @Test
    void lockReleasesAfterTheCooldown() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("user@example.com");
        }
        assertThatThrownBy(() -> service.assertNotLocked("user@example.com")).isInstanceOf(ApiException.class);

        clock.advance(Duration.ofMinutes(16));
        assertThatCode(() -> service.assertNotLocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulLoginResetsTheCounter() {
        service.recordFailure("user@example.com");
        service.recordFailure("user@example.com");
        service.recordSuccess("user@example.com");

        service.recordFailure("user@example.com");
        service.recordFailure("user@example.com");
        assertThatCode(() -> service.assertNotLocked("user@example.com")).doesNotThrowAnyException();
    }

    @Test
    void lockoutIsCaseInsensitiveOnEmail() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("User@Example.com");
        }
        assertThatThrownBy(() -> service.assertNotLocked("user@example.com")).isInstanceOf(ApiException.class);
    }
}
