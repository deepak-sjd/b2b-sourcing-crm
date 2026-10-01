package com.doctutorials.crm.customer;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.customer.dto.AddressDto;
import com.doctutorials.crm.customer.dto.CustomerContactRequest;
import com.doctutorials.crm.customer.dto.CustomerRequest;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final CustomerAccessService customerAccessService;
    private final CurrentUserService currentUserService;
    private final CustomerInsightService insightService;

    @Transactional
    public Customer create(CustomerRequest req) {
        Customer customer = new Customer();
        applyRequest(customer, req);
        customer.setAiHealthSummary(insightService.generateHealthSummary(customer));
        customer.setAiHealthGeneratedAt(Instant.now());

        Customer saved = customerRepository.save(customer);
        auditService.log("Customer", saved.getId(), AuditAction.CREATE, "Customer created: " + saved.getCompanyName());
        return saved;
    }

    @Transactional
    public Customer update(UUID id, CustomerRequest req) {
        Customer customer = getById(id);
        applyRequest(customer, req);
        customer.setAiHealthSummary(insightService.generateHealthSummary(customer));
        customer.setAiHealthGeneratedAt(Instant.now());

        Customer saved = customerRepository.save(customer);
        auditService.log("Customer", saved.getId(), AuditAction.UPDATE, "Customer details updated");
        return saved;
    }

    @Transactional
    public Customer changeApprovalStatus(UUID id, ApprovalStatus status) {
        Customer customer = getById(id);
        ApprovalStatus old = customer.getApprovalStatus();
        customer.setApprovalStatus(status);
        customer.setAiHealthSummary(insightService.generateHealthSummary(customer));

        Customer saved = customerRepository.save(customer);
        auditService.log("Customer", id, AuditAction.STATUS_CHANGE,
                "Approval status changed from " + old + " to " + status);
        return saved;
    }

    @Transactional
    public Customer assignAccountManager(UUID id, UUID accountManagerId) {
        Customer customer = getById(id);
        User manager = userRepository.findById(accountManagerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", accountManagerId));
        customerAccessService.assertCanAssignAccountManager(currentUserService.get(), manager);
        customer.setAccountManager(manager);

        Customer saved = customerRepository.save(customer);
        auditService.log("Customer", id, AuditAction.UPDATE, "Account manager reassigned to " + manager.getFullName());
        return saved;
    }

    @Transactional
    public void archive(UUID id) {
        Customer customer = getById(id);
        customer.setActive(false);
        customerRepository.save(customer);
        auditService.log("Customer", id, AuditAction.DELETE, "Customer archived");
    }

    public Customer getById(UUID id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
        customerAccessService.assertVisible(customer);
        return customer;
    }

    private Specification<Customer> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public Page<Customer> list(ApprovalStatus approvalStatus, UUID accountManagerId, Pageable pageable) {
        Specification<Customer> spec = Specification.where(customerAccessService.customerVisibility())
                .and(activeOnly());

        if (approvalStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("approvalStatus"), approvalStatus));
        }
        if (accountManagerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("accountManager").get("id"), accountManagerId));
        }
        return customerRepository.findAll(spec, pageable);
    }

    private void applyRequest(Customer customer, CustomerRequest req) {
        customer.setCompanyName(req.companyName().trim());
        customer.setGstNo(blankToNull(req.gstNo()));
        customer.setNatureOfBusiness(req.natureOfBusiness());

        applyAddress(customer.getBillingAddress(), req.billingAddress());

        customer.setShippingSameAsBilling(req.shippingSameAsBilling());
        if (req.shippingSameAsBilling()) {
            applyAddress(customer.getShippingAddress(), req.billingAddress());
        } else if (req.shippingAddress() != null) {
            applyAddress(customer.getShippingAddress(), req.shippingAddress());
        }

        if (req.approvalStatus() != null) {
            customer.setApprovalStatus(req.approvalStatus());
        }

        if (req.accountManagerId() != null) {
            User manager = userRepository.findById(req.accountManagerId())
                    .orElseThrow(() -> ResourceNotFoundException.of("User", req.accountManagerId()));
            customerAccessService.assertCanAssignAccountManager(currentUserService.get(), manager);
            customer.setAccountManager(manager);
        }

        syncContacts(customer, req);
    }

    /**
     * Adds/updates/removes CustomerContact rows to match the request — a row
     * with an id keeps its identity (edited in place), a row without one is
     * new, and any existing row whose id isn't present in the request is
     * dropped. Relies on orphanRemoval on Customer.contacts to turn a dropped
     * row into an actual DELETE on save.
     */
    private void syncContacts(Customer customer, CustomerRequest req) {
        if (req.contacts() == null) {
            return;
        }

        Set<UUID> requestedIds = req.contacts().stream()
                .map(CustomerContactRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        customer.getContacts().removeIf(existing -> !requestedIds.contains(existing.getId()));

        for (CustomerContactRequest cr : req.contacts()) {
            if (cr.id() != null) {
                customer.getContacts().stream()
                        .filter(c -> c.getId().equals(cr.id()))
                        .findFirst()
                        .ifPresent(existing -> {
                            existing.setContactType(cr.contactType());
                            existing.setName(cr.name());
                            existing.setEmail(cr.email());
                            existing.setContactNo(cr.contactNo());
                            existing.setDesignation(cr.designation());
                        });
            } else {
                CustomerContact newContact = new CustomerContact();
                newContact.setCustomer(customer);
                newContact.setContactType(cr.contactType());
                newContact.setName(cr.name());
                newContact.setEmail(cr.email());
                newContact.setContactNo(cr.contactNo());
                newContact.setDesignation(cr.designation());
                customer.getContacts().add(newContact);
            }
        }
    }

    private void applyAddress(Address target, AddressDto dto) {
        if (dto == null) return;
        target.setLine1(dto.line1());
        target.setLine2(dto.line2());
        target.setArea(dto.area());
        target.setCountry(dto.country());
        target.setState(dto.state());
        target.setCity(dto.city());
        target.setPinCode(dto.pinCode());
        target.setPhone(dto.phone());
        target.setEmail(dto.email());
        target.setGstNo(dto.gstNo());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
