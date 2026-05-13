package net.rafaelinfante.subscriptions.repository;

import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    Optional<Subscription> findByIdAndUserId(Long id, Long userId);

    @Query("""
        select s from Subscription s
        join fetch s.plan join fetch s.user
        where s.status in (net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus.ACTIVE,
                           net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus.TRIALING)
          and s.nextBillingDate <= :today
        """)
    List<Subscription> findDueForBilling(@Param("today") LocalDate today);

    @Query("""
        select s from Subscription s
        where s.cancelAtPeriodEnd = true
          and s.currentPeriodEnd <= :today
          and s.status <> :canceled
          and s.status <> :expired
        """)
    List<Subscription> findScheduledCancellationsDue(@Param("today") LocalDate today,
                                                      @Param("canceled") SubscriptionStatus canceled,
                                                      @Param("expired") SubscriptionStatus expired);
}
