package net.rafaelinfante.subscriptions.payment;

/**
 * Charges a payment method. A real implementation would call Stripe, Adyen, or similar;
 * this project ships a deterministic mock so the whole system runs without any keys.
 */
public interface PaymentProcessor {

    PaymentResult charge(ChargeRequest request);

    record ChargeRequest(Long userId, String paymentMethodToken, long amountCents,
                         String currency, String idempotencyKey) {
    }

    record PaymentResult(boolean success, String failureReason) {

        public static PaymentResult ok() {
            return new PaymentResult(true, null);
        }

        public static PaymentResult declined(String reason) {
            return new PaymentResult(false, reason);
        }
    }
}
