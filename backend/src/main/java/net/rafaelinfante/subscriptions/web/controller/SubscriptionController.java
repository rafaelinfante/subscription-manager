package net.rafaelinfante.subscriptions.web.controller;

import jakarta.validation.Valid;
import net.rafaelinfante.subscriptions.security.AuthUser;
import net.rafaelinfante.subscriptions.security.CurrentUser;
import net.rafaelinfante.subscriptions.service.SubscriptionLifecycleService;
import net.rafaelinfante.subscriptions.service.SubscriptionQueryService;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionLifecycleService lifecycle;
    private final SubscriptionQueryService query;

    public SubscriptionController(SubscriptionLifecycleService lifecycle, SubscriptionQueryService query) {
        this.lifecycle = lifecycle;
        this.query = query;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dtos.SubscriptionDto create(@CurrentUser AuthUser user,
                                       @Valid @RequestBody Dtos.CreateSubscriptionRequest request) {
        return lifecycle.subscribe(user.id(), request.planId(), request.paymentMethodToken(), request.trial());
    }

    @GetMapping
    public Page<Dtos.SubscriptionDto> list(@CurrentUser AuthUser user,
                                           @PageableDefault(size = 20, sort = "createdAt",
                                                   direction = Sort.Direction.DESC) Pageable pageable) {
        return query.listForUser(user.id(), pageable);
    }

    @GetMapping("/{id}")
    public Dtos.SubscriptionDto get(@CurrentUser AuthUser user, @PathVariable Long id) {
        return query.getForUser(user.id(), id);
    }

    @PutMapping("/{id}/plan")
    public Dtos.SubscriptionDto changePlan(@CurrentUser AuthUser user, @PathVariable Long id,
                                           @Valid @RequestBody Dtos.ChangePlanRequest request) {
        return lifecycle.changePlan(user.id(), id, request.planId());
    }

    @PostMapping("/{id}/cancel")
    public Dtos.SubscriptionDto cancel(@CurrentUser AuthUser user, @PathVariable Long id,
                                       @RequestParam(defaultValue = "false") boolean atPeriodEnd) {
        return lifecycle.cancel(user.id(), id, atPeriodEnd);
    }
}
