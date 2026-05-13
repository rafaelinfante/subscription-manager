package net.rafaelinfante.subscriptions.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** A single injectable clock keeps billing time-travel testable. */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
