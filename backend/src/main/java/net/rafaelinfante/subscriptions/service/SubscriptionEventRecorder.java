package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.SubscriptionEvent;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;
import net.rafaelinfante.subscriptions.repository.SubscriptionEventRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class SubscriptionEventRecorder {

    private final SubscriptionEventRepository events;
    private final Clock clock;

    public SubscriptionEventRecorder(SubscriptionEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    public void record(Subscription subscription, SubscriptionEventType type, String detail) {
        events.save(new SubscriptionEvent(subscription, type, detail, clock.instant()));
    }
}
