package net.rafaelinfante.subscriptions.web;

import com.jayway.jsonpath.JsonPath;
import net.rafaelinfante.subscriptions.AbstractIntegrationTest;
import net.rafaelinfante.subscriptions.repository.InvoiceRepository;
import net.rafaelinfante.subscriptions.repository.PlanRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionEventRepository;
import net.rafaelinfante.subscriptions.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionApiIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private PlanRepository plans;
    @Autowired private SubscriptionRepository subscriptions;
    @Autowired private InvoiceRepository invoices;
    @Autowired private SubscriptionEventRepository events;

    @BeforeEach
    void clean() {
        events.deleteAllInBatch();
        invoices.deleteAllInBatch();
        subscriptions.deleteAllInBatch();
    }

    @Test
    void subscribingChargesTheFirstPeriodAndRecordsAPaidInvoice() throws Exception {
        String token = demoToken();
        Long planId = plans.findByCode("pro_monthly").orElseThrow().getId();

        String created = mvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":%d,"paymentMethodToken":"tok_visa","trial":false}""".formatted(planId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan.code").value("pro_monthly"))
                .andReturn().getResponse().getContentAsString();
        long subscriptionId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mvc.perform(get("/api/invoices").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PAID"))
                .andExpect(jsonPath("$.content[0].amountCents").value(2999));

        mvc.perform(post("/api/subscriptions/" + subscriptionId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void subscribingRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":1,"trial":false}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void subscribingToAnUnknownPlanReturnsNotFound() throws Exception {
        String token = demoToken();
        mvc.perform(post("/api/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"planId":999999,"trial":false}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void aRegularUserCannotReachTheAdminApi() throws Exception {
        mvc.perform(post("/api/admin/billing/run").header("Authorization", "Bearer " + demoToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("forbidden"));
    }

    private String demoToken() throws Exception {
        String response = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"demo@demo.io","password":"Password123!"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }
}
