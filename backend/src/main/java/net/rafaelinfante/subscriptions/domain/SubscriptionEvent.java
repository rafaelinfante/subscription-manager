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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;

import java.time.Instant;

/** Append-only audit trail of everything that happens to a subscription. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_events")
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionEventType type;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public SubscriptionEvent(Subscription subscription, SubscriptionEventType type, String detail, Instant occurredAt) {
        this.subscription = subscription;
        this.type = type;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }
}
