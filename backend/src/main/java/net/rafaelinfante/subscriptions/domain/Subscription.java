package net.rafaelinfante.subscriptions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /** Mock payment method (e.g. tok_visa succeeds, tok_declined is declined). */
    @Column(name = "payment_method_token", nullable = false, length = 64)
    private String paymentMethodToken = "tok_visa";

    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private LocalDate currentPeriodEnd;

    @Column(name = "next_billing_date", nullable = false)
    private LocalDate nextBillingDate;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "failed_payment_attempts", nullable = false)
    private int failedPaymentAttempts = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Version
    private long version;
}
