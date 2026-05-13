package net.rafaelinfante.subscriptions.service;

import net.rafaelinfante.subscriptions.repository.InvoiceRepository;
import net.rafaelinfante.subscriptions.web.advice.ApiException;
import net.rafaelinfante.subscriptions.web.dto.DtoMapper;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InvoiceQueryService {

    private final InvoiceRepository invoices;

    public InvoiceQueryService(InvoiceRepository invoices) {
        this.invoices = invoices;
    }

    public Page<Dtos.InvoiceDto> listForUser(Long userId, Pageable pageable) {
        return invoices.findBySubscriptionUserId(userId, pageable).map(DtoMapper::toInvoiceDto);
    }

    public Dtos.InvoiceDto getForUser(Long userId, Long id) {
        return invoices.findByIdAndSubscriptionUserId(id, userId).map(DtoMapper::toInvoiceDto)
                .orElseThrow(() -> ApiException.notFound("Invoice not found"));
    }
}
