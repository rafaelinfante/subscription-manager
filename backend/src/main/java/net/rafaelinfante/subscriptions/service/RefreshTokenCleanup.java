package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Keeps the refresh-token table from growing forever by dropping tokens that have expired. */
@Component
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository tokens;
    private final Clock clock;

    public RefreshTokenCleanup(RefreshTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.refresh-token-cleanup.cron:0 30 3 * * *}", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        int removed = tokens.deleteExpiredBefore(clock.instant());
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }
}
