package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.ApiResponse;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.dto.CustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads/{id}")
@RequiredArgsConstructor
public class LeadConversionController {

    private final LeadConversionService leadConversionService;

    /** "Convert" vs. "View customer" on the frontend button without a 404 round-trip. */
    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> getConvertedCustomer(@PathVariable UUID id) {
        CustomerResponse response = leadConversionService.findConvertedCustomer(id)
                .map(CustomerResponse::from)
                .orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/convert-to-customer")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','MANAGER','COUNSELOR')")
    public ResponseEntity<ApiResponse<CustomerResponse>> convert(@PathVariable UUID id) {
        Customer customer = leadConversionService.convert(id);
        return ResponseEntity.ok(ApiResponse.ok("Lead converted to customer", CustomerResponse.from(customer)));
    }
}
