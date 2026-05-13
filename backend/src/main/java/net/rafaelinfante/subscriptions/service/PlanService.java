package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.domain.Plan;
import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import net.rafaelinfante.subscriptions.web.dto.DtoMapper;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanService {

    private final PlanRepository plans;

    public PlanService(PlanRepository plans) {
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public List<Dtos.PlanDto> listActive() {
        return plans.findByActiveTrueOrderByAmountCentsAsc().stream().map(DtoMapper::toPlanDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Dtos.PlanDto> listAll() {
        return plans.findAll().stream().map(DtoMapper::toPlanDto).toList();
    }

    @Transactional(readOnly = true)
    public Dtos.PlanDto get(Long id) {
        return plans.findById(id).map(DtoMapper::toPlanDto)
                .orElseThrow(() -> ApiException.notFound("Plan not found"));
    }

    @Transactional
    public Dtos.PlanDto create(Dtos.CreatePlanRequest request) {
        if (plans.findByCode(request.code()).isPresent()) {
            throw ApiException.conflict("plan_code_taken", "A plan with this code already exists");
        }
        Plan plan = new Plan();
        plan.setCode(request.code());
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setAmountCents(request.amountCents());
        plan.setCurrency(request.currency().toUpperCase());
        plan.setBillingInterval(request.billingInterval());
        plan.setActive(true);
        return DtoMapper.toPlanDto(plans.save(plan));
    }

    @Transactional
    public Dtos.PlanDto setActive(Long id, boolean active) {
        Plan plan = plans.findById(id).orElseThrow(() -> ApiException.notFound("Plan not found"));
        plan.setActive(active);
        return DtoMapper.toPlanDto(plans.save(plan));
    }
}
