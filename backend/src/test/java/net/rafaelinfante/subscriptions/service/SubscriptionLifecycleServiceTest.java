package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.Plan;
import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.BillingInterval;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;
import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 25);

    @Mock private SubscriptionRepository subscriptions;
    @Mock private UserRepository users;
    @Mock private PlanRepository plans;
    @Mock private BillingService billingService;
    @Mock private SubscriptionEventRecorder events;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-25T10:00:00Z"), ZoneOffset.UTC);

    private SubscriptionLifecycleService service() {
        return new SubscriptionLifecycleService(subscriptions, users, plans, billingService, events, clock);
    }

    @Test
    void subscribeWithoutTrialSetsUpFirstPeriodAndCharges() {
        when(users.findById(1L)).thenReturn(Optional.of(user()));
        when(plans.findById(10L)).thenReturn(Optional.of(monthlyPlan()));
        when(subscriptions.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        var dto = service().subscribe(1L, 10L, "tok_visa", false);

        assertThat(dto.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(dto.currentPeriodStart()).isEqualTo(TODAY);
        assertThat(dto.currentPeriodEnd()).isEqualTo(TODAY.plusMonths(1));
        assertThat(dto.nextBillingDate()).isEqualTo(TODAY.plusMonths(1));
        verify(billingService).billInitialPeriod(any());
    }

    @Test
    void subscribeWithTrialDoesNotChargeUpFront() {
        when(users.findById(1L)).thenReturn(Optional.of(user()));
        when(plans.findById(10L)).thenReturn(Optional.of(monthlyPlan()));
        when(subscriptions.save(any())).thenAnswer(inv -> {
            Subscription s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        var dto = service().subscribe(1L, 10L, "tok_visa", true);

        assertThat(dto.status()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(dto.trialEndDate()).isEqualTo(TODAY.plusDays(14));
        verify(billingService, never()).billInitialPeriod(any());
    }

    @Test
    void changePlanToTheSamePlanIsRejected() {
        Subscription subscription = activeSubscription();
        when(subscriptions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(subscription));
        when(plans.findById(10L)).thenReturn(Optional.of(subscription.getPlan()));

        assertThatThrownBy(() -> service().changePlan(1L, 5L, 10L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already on this plan");
    }

    @Test
    void changePlanOnACanceledSubscriptionIsRejected() {
        Subscription subscription = activeSubscription();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        when(subscriptions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service().changePlan(1L, 5L, 99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot change");
    }

    @Test
    void cancelImmediatelyMarksCanceled() {
        Subscription subscription = activeSubscription();
        when(subscriptions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(subscription));
        when(subscriptions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service().cancel(1L, 5L, false);

        assertThat(dto.status()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(dto.cancelAtPeriodEnd()).isFalse();
        verify(events).record(any(), org.mockito.ArgumentMatchers.eq(SubscriptionEventType.CANCELED), any());
    }

    @Test
    void cancelAtPeriodEndKeepsSubscriptionActive() {
        Subscription subscription = activeSubscription();
        when(subscriptions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(subscription));
        when(subscriptions.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = service().cancel(1L, 5L, true);

        assertThat(dto.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(dto.cancelAtPeriodEnd()).isTrue();
    }

    @Test
    void cancelingAnAlreadyCanceledSubscriptionIsRejected() {
        Subscription subscription = activeSubscription();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        when(subscriptions.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service().cancel(1L, 5L, false))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already canceled");
    }

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        return user;
    }

    private static Plan monthlyPlan() {
        Plan plan = new Plan();
        plan.setId(10L);
        plan.setCode("pro_monthly");
        plan.setAmountCents(2999);
        plan.setCurrency("USD");
        plan.setBillingInterval(BillingInterval.MONTH);
        plan.setActive(true);
        return plan;
    }

    private static Subscription activeSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(5L);
        subscription.setUser(user());
        subscription.setPlan(monthlyPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(TODAY);
        subscription.setCurrentPeriodEnd(TODAY.plusMonths(1));
        subscription.setNextBillingDate(TODAY.plusMonths(1));
        return subscription;
    }
}
