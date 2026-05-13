package net.rafaelinfante.subscriptions;

import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full context against a real MySQL so Flyway migrations run and Hibernate
 * validates every entity mapping against the actual schema.
 */
class SchemaValidationIT extends AbstractIntegrationTest {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void schemaMatchesEntitiesAndSeedDataLoads() {
        assertThat(planRepository.findByActiveTrueOrderByAmountCentsAsc()).hasSize(6);
        assertThat(userRepository.findByEmail("demo@demo.io")).isPresent();
        assertThat(userRepository.findByEmail("admin@demo.io"))
                .get()
                .extracting(u -> u.getRoles().contains("ADMIN"))
                .isEqualTo(true);
    }
}
