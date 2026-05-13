package net.rafaelinfante.subscriptions.web.controller;

import net.rafaelinfante.subscriptions.service.PlanService;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    @GetMapping
    public List<Dtos.PlanDto> list() {
        return plans.listActive();
    }

    @GetMapping("/{id}")
    public Dtos.PlanDto get(@PathVariable Long id) {
        return plans.get(id);
    }
}
