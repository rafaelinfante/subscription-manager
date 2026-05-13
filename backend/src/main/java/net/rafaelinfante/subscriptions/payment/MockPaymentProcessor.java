package net.rafaelinfante.subscriptions.payment;

import net.rafaelinfante.subscriptions.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stand-in payment gateway. In the default {@code token} mode the outcome is decided by the
 * subscription's payment method: {@code tok_declined} always fails, everything else succeeds.
 * That makes the dunning flow easy to demo and deterministic to test. The mode can be forced to
 * {@code success}, {@code failure}, or {@code random} through configuration.
 */
@Component
public class MockPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProcessor.class);
    private static final String DECLINED_TOKEN = "tok_declined";

    private final String mode;

    public MockPaymentProcessor(AppProperties properties) {
        this.mode = properties.payment().mockMode();
    }

    @Override
    public PaymentResult charge(ChargeRequest request) {
        PaymentResult result = switch (mode) {
            case "success" -> PaymentResult.ok();
            case "failure" -> PaymentResult.declined("card_declined");
            case "random" -> ThreadLocalRandom.current().nextInt(100) < 80
                    ? PaymentResult.ok()
                    : PaymentResult.declined("card_declined");
            default -> DECLINED_TOKEN.equals(request.paymentMethodToken())
                    ? PaymentResult.declined("card_declined")
                    : PaymentResult.ok();
        };
        log.debug("Mock charge of {} {} for user {} -> {}",
                request.amountCents(), request.currency(), request.userId(),
                result.success() ? "approved" : "declined");
        return result;
    }
}
