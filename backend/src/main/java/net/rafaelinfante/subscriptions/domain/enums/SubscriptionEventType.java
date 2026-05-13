package net.rafaelinfante.subscriptions.domain.enums;

public enum SubscriptionEventType {
    CREATED,
    PLAN_CHANGED,
    RENEWED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    MARKED_PAST_DUE,
    RECOVERED,
    CANCEL_SCHEDULED,
    CANCELED,
    EXPIRED
}
