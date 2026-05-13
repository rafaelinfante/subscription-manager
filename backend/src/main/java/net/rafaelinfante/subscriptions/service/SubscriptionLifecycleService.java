package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.Plan;
import net.rafaelinfante.subscriptions.domain.Subscription;
import net.rafaelinfante.subscriptions.domain.User;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionEventType;
import net.rafaelinfante.subscriptions.domain.enums.SubscriptionStatus;
import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import net.rafaelinfante.subscriptions.repository.UserRepository;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import net.rafaelinfante.subscriptions.web.dto.DtoMapper;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/** Owns the subscription state machine: create, change plan, and cancel. */
@Service
public class SubscriptionLifecycleService {

    private static final int TRIAL_DAYS = 14;
    private static final Set<SubscriptionStatus> MODIFIABLE =
            EnumSet.of(SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE);

    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final PlanRepository plans;
    private final BillingService billingService;
    private final SubscriptionEventRecorder events;
    private final Clock clock;

    public SubscriptionLifecycleService(SubscriptionRepository subscriptions, UserRepository users,
                                        PlanRepository plans, BillingService billingService,
                                        SubscriptionEventRecorder events, Clock clock) {
        this.subscriptions = subscriptions;
        this.users = users;
        this.plans = plans;
        this.billingService = billingService;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public Dtos.SubscriptionDto subscribe(Long userId, Long planId, String paymentMethodToken, boolean withTrial) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        Plan plan = activePlan(planId);

        LocalDate today = LocalDate.now(clock);
        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setPaymentMethodToken(paymentMethodToken == null || paymentMethodToken.isBlank()
                ? "tok_visa" : paymentMethodToken);
        subscription.setCurrentPeriodStart(today);

        if (withTrial) {
            LocalDate trialEnd = today.plusDays(TRIAL_DAYS);
            subscription.setStatus(SubscriptionStatus.TRIALING);
            subscription.setTrialEndDate(trialEnd);
            subscription.setCurrentPeriodEnd(trialEnd);
            subscription.setNextBillingDate(trialEnd);
            subscriptions.save(subscription);
            events.record(subscription, SubscriptionEventType.CREATED, "Trial started until " + trialEnd);
        } else {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodEnd(plan.getBillingInterval().advance(today));
            subscription.setNextBillingDate(subscription.getCurrentPeriodEnd());
            subscriptions.save(subscription);
            events.record(subscription, SubscriptionEventType.CREATED, "Subscribed to " + plan.getCode());
            billingService.billInitialPeriod(subscription);
        }
        return DtoMapper.toSubscriptionDto(subscription);
    }

    @Transactional
    public Dtos.SubscriptionDto changePlan(Long userId, Long subscriptionId, Long newPlanId) {
        Subscription subscription = ownedSubscription(userId, subscriptionId);
        requireModifiable(subscription);
        Plan newPlan = activePlan(newPlanId);
        if (newPlan.getId().equals(subscription.getPlan().getId())) {
            throw ApiException.badRequest("same_plan", "Subscription is already on this plan");
        }
        String from = subscription.getPlan().getCode();
        subscription.setPlan(newPlan);
        events.record(subscription, SubscriptionEventType.PLAN_CHANGED,
                "Changed from %s to %s; new price applies from the next renewal".formatted(from, newPlan.getCode()));
        return DtoMapper.toSubscriptionDto(subscriptions.save(subscription));
    }

    @Transactional
    public Dtos.SubscriptionDto cancel(Long userId, Long subscriptionId, boolean atPeriodEnd) {
        Subscription subscription = ownedSubscription(userId, subscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELED
                || subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            throw ApiException.badRequest("already_ended", "Subscription is already canceled");
        }
        if (atPeriodEnd) {
            subscription.setCancelAtPeriodEnd(true);
            events.record(subscription, SubscriptionEventType.CANCEL_SCHEDULED,
                    "Will end on " + subscription.getCurrentPeriodEnd());
        } else {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscription.setCancelAtPeriodEnd(false);
            subscription.setCanceledAt(clock.instant());
            events.record(subscription, SubscriptionEventType.CANCELED, "Canceled immediately");
        }
        return DtoMapper.toSubscriptionDto(subscriptions.save(subscription));
    }

    private Subscription ownedSubscription(Long userId, Long subscriptionId) {
        return subscriptions.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
    }

    private Plan activePlan(Long planId) {
        return plans.findById(planId)
                .filter(Plan::isActive)
                .orElseThrow(() -> ApiException.notFound("Plan not found or inactive"));
    }

    private void requireModifiable(Subscription subscription) {
        if (!MODIFIABLE.contains(subscription.getStatus())) {
            throw ApiException.badRequest("not_modifiable",
                    "Cannot change a subscription that is " + subscription.getStatus().name().toLowerCase());
        }
    }
}
