package com.doctutorials.crm.customer;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.customer.dto.CustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exposes POST /api/v1/leads/{id}/convert-to-customer - same URL prefix as
 * LeadController, but deliberately a separate controller living here in the
 * "customer" package instead.
 *
 * Why: LeadConversionService needs both Lead and Customer. "customer"
 * already depends on "lead" nowhere else forces the reverse - if this
 * endpoint lived on LeadController instead, "lead" would need to import
 * Customer types too, and the two packages would depend on each other in
 * both directions. Keeping it here keeps the dependency one-way (customer
 * -> lead only), same discipline as TaskAccessService/CallAccessService
 * living in their own packages instead of inside "lead" (see those
 * classes' comments from the row-level-security work).
 */
@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadConversionController {

    private final LeadConversionService leadConversionService;

    /** Only valid once the lead is WON - see LeadConversionService. Idempotent:
     *  calling this again just returns the customer already created the first time. */
    @PostMapping("/{id}/convert-to-customer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<CustomerResponse>> convertToCustomer(@PathVariable UUID id) {
        Customer customer = leadConversionService.convert(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead converted to customer", CustomerResponse.from(customer)));
    }
}
