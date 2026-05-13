package net.rafaelinfante.subscriptions.payment;

import net.rafaelinfante.subscriptions.config.AppProperties;
import net.rafaelinfante.subscriptions.payment.PaymentProcessor.ChargeRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPaymentProcessorTest {

    private MockPaymentProcessor processorWithMode(String mode) {
        AppProperties props = new AppProperties(null, null, null,
                new AppProperties.Payment(mode), null, null);
        return new MockPaymentProcessor(props);
    }

    private ChargeRequest charge(String token) {
        return new ChargeRequest(1L, token, 2999, "USD", "INV-1");
    }

    @Test
    void tokenModeApprovesNormalCardsAndDeclinesTheDeclinedToken() {
        MockPaymentProcessor processor = processorWithMode("token");

        assertThat(processor.charge(charge("tok_visa")).success()).isTrue();
        assertThat(processor.charge(charge("tok_declined")).success()).isFalse();
    }

    @Test
    void forcedModesOverrideTheToken() {
        assertThat(processorWithMode("success").charge(charge("tok_declined")).success()).isTrue();
        assertThat(processorWithMode("failure").charge(charge("tok_visa")).success()).isFalse();
    }
}
