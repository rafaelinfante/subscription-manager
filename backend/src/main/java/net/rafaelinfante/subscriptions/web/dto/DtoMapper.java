package net.rafaelinfante.subscriptions.web.dto;

import net.rafaelinfante.subscriptions.domain.Invoice;
import net.rafaelinfante.subscriptions.domain.Plan;
import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.User;

import java.util.Set;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static Dtos.UserDto toUserDto(User user) {
        return new Dtos.UserDto(user.getId(), user.getEmail(), user.getName(), Set.copyOf(user.getRoles()));
    }

    public static Dtos.PlanDto toPlanDto(Plan plan) {
        return new Dtos.PlanDto(plan.getId(), plan.getCode(), plan.getName(), plan.getDescription(),
                plan.getAmountCents(), plan.getCurrency(), plan.getBillingInterval(), plan.isActive());
    }

    public static Dtos.SubscriptionDto toSubscriptionDto(Subscription s) {
        return new Dtos.SubscriptionDto(s.getId(), toPlanDto(s.getPlan()), s.getStatus(), s.getPaymentMethodToken(),
                s.getTrialEndDate(), s.getCurrentPeriodStart(), s.getCurrentPeriodEnd(), s.getNextBillingDate(),
                s.isCancelAtPeriodEnd(), s.getCreatedAt());
    }

    public static Dtos.InvoiceDto toInvoiceDto(Invoice i) {
        return new Dtos.InvoiceDto(i.getId(), i.getNumber(), i.getStatus(), i.getAmountCents(), i.getCurrency(),
                i.getPeriodStart(), i.getPeriodEnd(), i.getAttemptCount(), i.getFailureReason(),
                i.getIssuedAt(), i.getPaidAt(), i.getNextRetryAt());
    }
}
