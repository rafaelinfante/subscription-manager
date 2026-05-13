package net.rafaelinfante.subscriptions.repository;

import net.rafaelinfante.subscriptions.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    List<Plan> findByActiveTrueOrderByAmountCentsAsc();
}
