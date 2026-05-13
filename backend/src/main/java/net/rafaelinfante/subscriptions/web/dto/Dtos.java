package net.rafaelinfante.subscriptions.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import net.rafaelinfante.subscriptions.domain.enums.BillingInterval;
import net.rafaelinfante.subscriptions.domain.enums.InvoiceStatus;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Request and response payloads for the REST API, grouped to keep the wire contract in one place. */
public final class Dtos {

    private Dtos() {
    }

    // --- Requests ---

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank String name) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record CreateSubscriptionRequest(
            @NotNull Long planId,
            String paymentMethodToken,
            boolean trial) {
    }

    public record ChangePlanRequest(@NotNull Long planId) {
    }

    public record CreatePlanRequest(
            @NotBlank String code,
            @NotBlank String name,
            String description,
            @Positive long amountCents,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull BillingInterval billingInterval) {
    }

    // --- Responses ---

    public record AuthResponse(String accessToken, UserDto user) {
    }

    public record UserDto(Long id, String email, String name, Set<String> roles) {
    }

    public record PlanDto(Long id, String code, String name, String description,
                          long amountCents, String currency, BillingInterval billingInterval, boolean active) {
    }

    public record SubscriptionDto(Long id, PlanDto plan, SubscriptionStatus status, String paymentMethodToken,
                                  LocalDate trialEndDate, LocalDate currentPeriodStart, LocalDate currentPeriodEnd,
                                  LocalDate nextBillingDate, boolean cancelAtPeriodEnd, Instant createdAt) {
    }

    public record InvoiceDto(Long id, String number, InvoiceStatus status, long amountCents, String currency,
                             LocalDate periodStart, LocalDate periodEnd, int attemptCount, String failureReason,
                             Instant issuedAt, Instant paidAt, Instant nextRetryAt) {
    }

    public record SocialProvidersResponse(List<String> providers) {
    }
}
