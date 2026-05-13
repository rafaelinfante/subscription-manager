package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.Invoice;
import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.enums.InvoiceStatus;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;
import net.rafaelinfante.subscriptions.payment.PaymentProcessor;
import net.rafaelinfante.subscriptions.payment.PaymentProcessor.ChargeRequest;
import net.rafaelinfante.subscriptions.payment.PaymentProcessor.PaymentResult;
import net.rafaelinfante.subscriptions.repository.InvoiceRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Runs the recurring charge cycle and the dunning process for failed payments. A failed charge
 * moves the subscription to {@code PAST_DUE} and schedules retries on a 1/3/5 day backoff; once
 * the retries are exhausted the subscription is canceled.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final PaymentProcessor paymentProcessor;
    private final SubscriptionEventRecorder events;
    private final Clock clock;

    /** Days to wait before each retry; its length is also the maximum number of retries. */
    private final int[] retryBackoffDays;

    public BillingService(SubscriptionRepository subscriptions, InvoiceRepository invoices,
                          PaymentProcessor paymentProcessor, SubscriptionEventRecorder events, Clock clock,
                          @Value("${app.billing.retry-backoff-days:1,3,5}") int[] retryBackoffDays) {
        this.subscriptions = subscriptions;
        this.invoices = invoices;
        this.paymentProcessor = paymentProcessor;
        this.events = events;
        this.clock = clock;
        this.retryBackoffDays = retryBackoffDays;
    }

    /** Daily sweep: end scheduled cancellations, charge renewals, and retry failed payments. */
    @Scheduled(cron = "${app.billing.cron:0 0 2 * * *}", zone = "UTC")
    @Transactional
    public void runBillingCycle() {
        LocalDate today = LocalDate.now(clock);
        log.info("Running billing cycle for {}", today);

        for (Subscription sub : subscriptions.findScheduledCancellationsDue(
                today, SubscriptionStatus.CANCELED, SubscriptionStatus.EXPIRED)) {
            safely("expire", sub.getId(), () -> expire(sub));
        }
        for (Subscription sub : subscriptions.findDueForBilling(today)) {
            safely("renew", sub.getId(), () -> renew(sub));
        }
        for (Invoice invoice : invoices.findDueForRetry(clock.instant())) {
            safely("retry", invoice.getId(), () -> attemptCharge(invoice));
        }
    }

    /** Charges the first period when a subscription is created without a trial. */
    public Invoice billInitialPeriod(Subscription subscription) {
        Invoice invoice = createInvoice(subscription, subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd());
        attemptCharge(invoice);
        return invoice;
    }

    private void renew(Subscription subscription) {
        subscription.setCurrentPeriodStart(subscription.getCurrentPeriodEnd());
        subscription.setCurrentPeriodEnd(
                subscription.getPlan().getBillingInterval().advance(subscription.getCurrentPeriodEnd()));
        events.record(subscription, SubscriptionEventType.RENEWED,
                "Renewed for %s to %s".formatted(subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd()));
        Invoice invoice = createInvoice(subscription, subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd());
        attemptCharge(invoice);
    }

    private void expire(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptions.save(subscription);
        events.record(subscription, SubscriptionEventType.EXPIRED, "Ended at the close of the billing period");
    }

    private void attemptCharge(Invoice invoice) {
        Subscription subscription = invoice.getSubscription();
        invoice.setAttemptCount(invoice.getAttemptCount() + 1);

        PaymentResult result = paymentProcessor.charge(new ChargeRequest(
                subscription.getUser().getId(), subscription.getPaymentMethodToken(),
                invoice.getAmountCents(), invoice.getCurrency(), invoice.getNumber()));

        if (result.success()) {
            settlePaid(invoice, subscription);
        } else {
            settleFailed(invoice, subscription, result.failureReason());
        }
    }

    private void settlePaid(Invoice invoice, Subscription subscription) {
        boolean recovered = subscription.getStatus() == SubscriptionStatus.PAST_DUE;

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(clock.instant());
        invoice.setFailureReason(null);
        invoice.setNextRetryAt(null);
        invoices.save(invoice);

        subscription.setFailedPaymentAttempts(0);
        subscription.setNextRetryAt(null);
        if (subscription.getStatus() == SubscriptionStatus.TRIALING
                || subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }
        subscription.setNextBillingDate(subscription.getCurrentPeriodEnd());
        subscriptions.save(subscription);

        events.record(subscription, SubscriptionEventType.PAYMENT_SUCCEEDED, "Invoice " + invoice.getNumber() + " paid");
        if (recovered) {
            events.record(subscription, SubscriptionEventType.RECOVERED, "Recovered after a successful retry");
        }
    }

    private void settleFailed(Invoice invoice, Subscription subscription, String reason) {
        invoice.setFailureReason(reason);
        subscription.setFailedPaymentAttempts(subscription.getFailedPaymentAttempts() + 1);
        events.record(subscription, SubscriptionEventType.PAYMENT_FAILED,
                "Invoice " + invoice.getNumber() + " failed: " + reason);

        if (subscription.getFailedPaymentAttempts() <= retryBackoffDays.length) {
            Instant retryAt = clock.instant().plus(
                    Duration.ofDays(retryBackoffDays[subscription.getFailedPaymentAttempts() - 1]));
            invoice.setStatus(InvoiceStatus.FAILED);
            invoice.setNextRetryAt(retryAt);
            subscription.setNextRetryAt(retryAt);
            if (subscription.getStatus() != SubscriptionStatus.PAST_DUE) {
                subscription.setStatus(SubscriptionStatus.PAST_DUE);
                events.record(subscription, SubscriptionEventType.MARKED_PAST_DUE, "Payment past due; retry scheduled");
            }
        } else {
            invoice.setStatus(InvoiceStatus.VOID);
            invoice.setNextRetryAt(null);
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCanceledAt(clock.instant());
            subscription.setNextRetryAt(null);
            events.record(subscription, SubscriptionEventType.CANCELED, "Canceled after exhausting payment retries");
        }
        invoices.save(invoice);
        subscriptions.save(subscription);
    }

    private Invoice createInvoice(Subscription subscription, LocalDate periodStart, LocalDate periodEnd) {
        Invoice invoice = new Invoice();
        invoice.setSubscription(subscription);
        invoice.setNumber("INV-%d-%s".formatted(subscription.getId(), periodStart.format(DateTimeFormatter.BASIC_ISO_DATE)));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setAmountCents(subscription.getPlan().getAmountCents());
        invoice.setCurrency(subscription.getPlan().getCurrency());
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setIssuedAt(clock.instant());
        return invoices.save(invoice);
    }

    private void safely(String action, Long id, Runnable work) {
        try {
            work.run();
        } catch (Exception e) {
            log.error("Billing step {} failed for {}: {}", action, id, e.getMessage(), e);
        }
    }
}
