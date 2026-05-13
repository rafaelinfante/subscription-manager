package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import net.rafaelinfante.subscriptions.web.dto.DtoMapper;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptions;

    public SubscriptionQueryService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Page<Dtos.SubscriptionDto> listForUser(Long userId, Pageable pageable) {
        return subscriptions.findByUserId(userId, pageable).map(DtoMapper::toSubscriptionDto);
    }

    public Dtos.SubscriptionDto getForUser(Long userId, Long id) {
        return subscriptions.findByIdAndUserId(id, userId).map(DtoMapper::toSubscriptionDto)
                .orElseThrow(() -> ApiException.notFound("Subscription not found"));
    }

    public Page<Dtos.SubscriptionDto> listAll(Pageable pageable) {
        return subscriptions.findAll(pageable).map(DtoMapper::toSubscriptionDto);
    }
}
