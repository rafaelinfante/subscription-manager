package net.rafaelinfante.subscriptions.web.controller;

import jakarta.validation.Valid;
import net.rafaelinfante.subscriptions.service.BillingService;
import net.rafaelinfante.subscriptions.service.PlanService;
import net.rafaelinfante.subscriptions.service.SubscriptionQueryService;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PlanService plans;
    private final SubscriptionQueryService subscriptions;
    private final BillingService billing;

    public AdminController(PlanService plans, SubscriptionQueryService subscriptions, BillingService billing) {
        this.plans = plans;
        this.subscriptions = subscriptions;
        this.billing = billing;
    }

    @GetMapping("/plans")
    public List<Dtos.PlanDto> listPlans() {
        return plans.listAll();
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.PlanDto createPlan(@Valid @RequestBody Dtos.CreatePlanRequest request) {
        return plans.create(request);
    }

    @PostMapping("/plans/{id}/activate")
    public Dtos.PlanDto activatePlan(@PathVariable Long id) {
        return plans.setActive(id, true);
    }

    @PostMapping("/plans/{id}/deactivate")
    public Dtos.PlanDto deactivatePlan(@PathVariable Long id) {
        return plans.setActive(id, false);
    }

    @GetMapping("/subscriptions")
    public Page<Dtos.SubscriptionDto> listSubscriptions(@PageableDefault(size = 20) Pageable pageable) {
        return subscriptions.listAll(pageable);
    }

    /** Runs the billing/dunning sweep on demand, which makes the lifecycle easy to demo. */
    @PostMapping("/billing/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void runBilling() {
        billing.runBillingCycle();
    }
}
