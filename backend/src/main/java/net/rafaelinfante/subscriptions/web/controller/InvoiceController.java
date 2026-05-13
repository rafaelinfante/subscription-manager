package net.rafaelinfante.subscriptions.web.controller;

import net.rafaelinfante.subscriptions.security.AuthUser;
import net.rafaelinfante.subscriptions.security.CurrentUser;
import net.rafaelinfante.subscriptions.service.InvoiceQueryService;
import net.rafaelinfante.subscriptions.web.dto.Dtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceQueryService invoices;

    public InvoiceController(InvoiceQueryService invoices) {
        this.invoices = invoices;
    }

    @GetMapping
    public Page<Dtos.InvoiceDto> list(@CurrentUser AuthUser user,
                                      @PageableDefault(size = 20, sort = "issuedAt",
                                              direction = Sort.Direction.DESC) Pageable pageable) {
        return invoices.listForUser(user.id(), pageable);
    }

    @GetMapping("/{id}")
    public Dtos.InvoiceDto get(@CurrentUser AuthUser user, @PathVariable Long id) {
        return invoices.getForUser(user.id(), id);
    }
}
