package net.rafaelinfante.subscriptions;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for integration tests. Uses a single MySQL container shared across the whole suite
 * (started once, reused) so the tests run fast and Flyway seeds the schema only once.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.billing.cron=-")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"));

    static {
        MYSQL.start();
    }
}
