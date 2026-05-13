package net.rafaelinfante.subscriptions.repository;

import net.rafaelinfante.subscriptions.domain.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findBySubscriptionUserId(Long userId, Pageable pageable);

    Optional<Invoice> findByIdAndSubscriptionUserId(Long id, Long userId);

    @Query("""
        select i from Invoice i
        join fetch i.subscription s join fetch s.user join fetch s.plan
        where i.status = net.rafaelinfante.subscriptions.domain.enums.InvoiceStatus.FAILED
          and i.nextRetryAt is not null
          and i.nextRetryAt <= :now
        """)
    List<Invoice> findDueForRetry(@Param("now") Instant now);
}
