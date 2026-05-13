package net.rafaelinfante.subscriptions.repository;

import net.rafaelinfante.subscriptions.domain.SubscriptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionEventRepository extends JpaRepository<SubscriptionEvent, Long> {

    List<SubscriptionEvent> findBySubscriptionIdOrderByOccurredAtDesc(Long subscriptionId);
}
