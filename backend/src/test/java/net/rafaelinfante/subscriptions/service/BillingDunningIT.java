package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.AbstractIntegrationTest;
import net.rafaelinfante.subscriptions.domain.Invoice;
import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.enums.InvoiceStatus;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;
import net.rafaelinfante.subscriptions.repository.InvoiceRepository;
import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionEventRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import net.rafaelinfante.subscriptions.support.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(BillingDunningIT.FixedClockConfig.class)
class BillingDunningIT extends AbstractIntegrationTest {

    private static final Instant BASE = Instant.parse("2026-06-25T12:00:00Z");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return MutableClock.at(BASE);
        }
    }

    @Autowired private SubscriptionLifecycleService lifecycle;
    @Autowired private BillingService billing;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private InvoiceRepository invoices;
    @Autowired private SubscriptionEventRepository events;
    @Autowired private UserRepository users;
    @Autowired private PlanRepository plans;
    @Autowired private MutableClock clock;

    private Long userId;
    private Long planId;

    @BeforeEach
    void setUp() {
        events.deleteAllInBatch();
        invoices.deleteAllInBatch();
        subscriptions.deleteAllInBatch();
        clock.setInstant(BASE);
        userId = users.findByEmail("demo@demo.io").orElseThrow().getId();
        planId = plans.findByCode("pro_monthly").orElseThrow().getId();
    }

    @Test
    void aHealthyCardRenewsAndPaysEachPeriod() {
        var dto = lifecycle.subscribe(userId, planId, "tok_visa", false);
        assertThat(dto.status()).isEqualTo(SubscriptionStatus.ACTIVE);

        Subscription subscription = reload(dto.id());
        LocalDate firstPeriodEnd = subscription.getCurrentPeriodEnd();
        assertThat(invoicesForUser()).singleElement()
                .extracting(Invoice::getStatus).isEqualTo(InvoiceStatus.PAID);

        moveTo(subscription.getNextBillingDate());
        billing.runBillingCycle();

        Subscription renewed = reload(dto.id());
        assertThat(renewed.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(renewed.getCurrentPeriodStart()).isEqualTo(firstPeriodEnd);
        assertThat(renewed.getCurrentPeriodEnd()).isEqualTo(firstPeriodEnd.plusMonths(1));
        assertThat(invoicesForUser()).hasSize(2)
                .allMatch(invoice -> invoice.getStatus() == InvoiceStatus.PAID);
    }

    @Test
    void aDecliningCardIsRetriedOnBackoffThenCanceled() {
        var dto = lifecycle.subscribe(userId, planId, "tok_declined", false);

        Subscription subscription = reload(dto.id());
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(subscription.getFailedPaymentAttempts()).isEqualTo(1);
        assertThat(invoicesForUser()).singleElement()
                .extracting(Invoice::getStatus).isEqualTo(InvoiceStatus.FAILED);

        // Three scheduled retries on a 1/3/5 day backoff, all declining.
        for (int expectedAttempts = 2; expectedAttempts <= 4; expectedAttempts++) {
            moveToRetry(reload(dto.id()));
            billing.runBillingCycle();
            assertThat(reload(dto.id()).getFailedPaymentAttempts()).isEqualTo(expectedAttempts);
        }

        Subscription canceled = reload(dto.id());
        assertThat(canceled.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(invoicesForUser()).singleElement()
                .extracting(Invoice::getStatus).isEqualTo(InvoiceStatus.VOID);
    }

    @Test
    void aPastDueSubscriptionRecoversWhenTheCardWorksAgain() {
        var dto = lifecycle.subscribe(userId, planId, "tok_declined", false);
        Subscription pastDue = reload(dto.id());
        assertThat(pastDue.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);

        pastDue.setPaymentMethodToken("tok_visa");
        subscriptions.save(pastDue);

        moveToRetry(reload(dto.id()));
        billing.runBillingCycle();

        Subscription recovered = reload(dto.id());
        assertThat(recovered.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(recovered.getFailedPaymentAttempts()).isZero();
        assertThat(invoicesForUser()).singleElement()
                .extracting(Invoice::getStatus).isEqualTo(InvoiceStatus.PAID);
        assertThat(events.findBySubscriptionIdOrderByOccurredAtDesc(recovered.getId()))
                .anyMatch(event -> event.getType() == SubscriptionEventType.RECOVERED);
    }

    private Subscription reload(Long id) {
        return subscriptions.findById(id).orElseThrow();
    }

    private List<Invoice> invoicesForUser() {
        return invoices.findBySubscriptionUserId(userId, Pageable.unpaged()).getContent();
    }

    private void moveTo(LocalDate date) {
        clock.setInstant(date.atTime(12, 0).toInstant(ZoneOffset.UTC));
    }

    private void moveToRetry(Subscription subscription) {
        clock.setInstant(subscription.getNextRetryAt().plusSeconds(1));
    }
}
