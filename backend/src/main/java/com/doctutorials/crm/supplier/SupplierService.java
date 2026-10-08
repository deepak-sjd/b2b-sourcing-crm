package com.doctutorials.crm.supplier;

import com.doctutorials.crm.audit.AuditAction;
import com.doctutorials.crm.audit.AuditService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.dto.SupplierRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierScoringService scoringService;
    private final SupplierDuplicateService duplicateService;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    @Transactional
    public Supplier create(SupplierRequest req) {
        Supplier supplier = new Supplier();
        applyRequest(supplier, req);
        supplier.setCreatedBy(currentUserService.get());
        assertNoBlockingDuplicate(req, null);

        scoringService.rescore(supplier);
        Supplier saved = supplierRepository.save(supplier);
        saved.setSupplierCode(generateCode(saved));
        saved = supplierRepository.save(saved);

        auditService.log("Supplier", saved.getId(), AuditAction.CREATE,
                "Supplier created: " + saved.getCompanyName());
        return saved;
    }

    @Transactional
    public Supplier update(UUID id, SupplierRequest req) {
        Supplier supplier = getById(id);
        Map<String, Object> before = snapshotForUpdate(supplier);
        applyRequest(supplier, req);
        assertNoBlockingDuplicate(req, id);

        scoringService.rescore(supplier);
        Supplier saved = supplierRepository.save(supplier);
        // accountNumber in the snapshot is masked automatically by
        // AuditService.logChange (key-name match), not here.
        auditService.logChange("Supplier", saved.getId(), AuditAction.UPDATE, "Supplier details updated",
                before, snapshotForUpdate(saved));
        return saved;
    }

    @Transactional
    public Supplier changeStatus(UUID id, SupplierStatus newStatus) {
        Supplier supplier = getById(id);
        SupplierStatus old = supplier.getStatus();
        supplier.setStatus(newStatus);
        Supplier saved = supplierRepository.save(supplier);
        auditService.logChange("Supplier", id, AuditAction.STATUS_CHANGE,
                "Status changed from " + old + " to " + newStatus,
                Map.of("status", old), Map.of("status", newStatus));
        return saved;
    }

    @Transactional
    public void archive(UUID id) {
        Supplier supplier = getById(id);
        supplier.setArchived(true);
        supplierRepository.save(supplier);
        auditService.log("Supplier", id, AuditAction.DELETE, "Supplier archived");
    }

    public Supplier getById(UUID id) {
        return supplierRepository.findById(id)
                .filter(s -> !s.isArchived())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    public Page<Supplier> list(String search, SupplierStatus status, SupplierCategory category, Pageable pageable) {
        Specification<Supplier> spec = Specification.where(notArchived());

        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (category != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (search != null && !search.isBlank()) {
            String like = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("companyName")), like),
                    cb.like(cb.lower(root.get("supplierCode")), like),
                    cb.like(cb.lower(root.get("gstNumber")), like),
                    cb.like(cb.lower(root.get("contactName")), like),
                    cb.like(cb.lower(root.get("contactPhone")), like),
                    cb.like(cb.lower(root.get("contactEmail")), like)));
        }
        return supplierRepository.findAll(spec, pageable);
    }

    public List<SupplierDuplicateService.DuplicateMatch> checkDuplicates(String companyName, String gstNumber,
            String contactPhone, UUID excludeId) {
        return duplicateService.findPotentialDuplicates(companyName, gstNumber, contactPhone, excludeId);
    }

    public GstUtil.GstDetails lookupGst(String gstin) {
        return GstUtil.parse(gstin);
    }

    public SupplierSummary summary() {
        List<Supplier> all = supplierRepository.findAll(notArchived());
        long verified = all.stream().filter(s -> s.getStatus() == SupplierStatus.VERIFIED).count();
        long pending = all.stream().filter(s -> s.getStatus() == SupplierStatus.PENDING_VERIFICATION).count();
        long onHold = all.stream().filter(s -> s.getStatus() == SupplierStatus.ON_HOLD
                || s.getStatus() == SupplierStatus.BLACKLISTED).count();
        double avgScore = all.stream().mapToInt(Supplier::getTrustScore).average().orElse(0);
        return new SupplierSummary(all.size(), verified, pending, onHold, Math.round(avgScore));
    }

    public record SupplierSummary(long total, long verified, long pendingVerification, long onHoldOrBlacklisted,
            long averageTrustScore) {}

    private Specification<Supplier> notArchived() {
        return (root, query, cb) -> cb.isFalse(root.get("archived"));
    }

    /**
     * Exact GST match is a hard stop, not a warning - unlike a similar-looking
     * name (which a human should judge), the same GSTIN legitimately cannot
     * belong to two different suppliers, so this one is enforced server-side
     * regardless of whether the UI's soft duplicate check ran first.
     */
    private void assertNoBlockingDuplicate(SupplierRequest req, UUID excludeId) {
        if (req.gstNumber() == null || req.gstNumber().isBlank()) return;
        supplierRepository.findByGstNumberIgnoreCaseAndArchivedFalse(req.gstNumber().trim())
                .filter(existing -> excludeId == null || !existing.getId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "A supplier with this GST number already exists: " + existing.getCompanyName());
                });
    }

    private void applyRequest(Supplier supplier, SupplierRequest req) {
        supplier.setCompanyName(req.companyName().trim());
        supplier.setCategory(req.category());

        String gst = req.gstNumber() != null ? req.gstNumber().trim().toUpperCase() : null;
        supplier.setGstNumber(gst);
        if (gst != null && !gst.isBlank()) {
            GstUtil.GstDetails details = GstUtil.parse(gst);
            supplier.setGstVerified(details.isFullyValid());
            if (details.isFullyValid()) {
                supplier.setState(supplier.getState() == null || supplier.getState().isBlank()
                        ? details.stateName() : supplier.getState());
                supplier.setPanNumber(req.panNumber() != null && !req.panNumber().isBlank()
                        ? req.panNumber().trim().toUpperCase() : details.panNumber());
            } else {
                supplier.setPanNumber(req.panNumber());
            }
        } else {
            supplier.setGstVerified(false);
            supplier.setPanNumber(req.panNumber());
        }

        supplier.setAddressLine1(req.addressLine1());
        supplier.setAddressLine2(req.addressLine2());
        supplier.setCountry(req.country() == null || req.country().isBlank() ? "India" : req.country());
        if (req.state() != null && !req.state().isBlank()) supplier.setState(req.state());
        supplier.setCity(req.city());
        supplier.setPincode(req.pincode());

        supplier.setBankName(req.bankName());
        supplier.setBankAddress(req.bankAddress());
        supplier.setAccountHolderName(req.accountHolderName());
        supplier.setAccountNumber(req.accountNumber());
        supplier.setIfscCode(req.ifscCode() != null ? req.ifscCode().toUpperCase() : null);

        supplier.setContactName(req.contactName());
        supplier.setContactDesignation(req.contactDesignation());
        supplier.setContactEmail(req.contactEmail());
        supplier.setContactPhone(req.contactPhone());

        supplier.setPaymentTerms(req.paymentTerms());
        supplier.setProducts(req.products() != null ? new ArrayList<>(req.products()) : new ArrayList<>());
    }

    /**
     * Snapshot of the fields an update can change, used for the audit
     * before/after diff. Uses LinkedHashMap because Map.of() rejects null
     * values, which many of these fields legitimately have.
     */
    private Map<String, Object> snapshotForUpdate(Supplier s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("companyName", s.getCompanyName());
        map.put("category", s.getCategory());
        map.put("gstNumber", s.getGstNumber());
        map.put("panNumber", s.getPanNumber());
        map.put("addressLine1", s.getAddressLine1());
        map.put("addressLine2", s.getAddressLine2());
        map.put("country", s.getCountry());
        map.put("state", s.getState());
        map.put("city", s.getCity());
        map.put("pincode", s.getPincode());
        map.put("bankName", s.getBankName());
        map.put("bankAddress", s.getBankAddress());
        map.put("accountHolderName", s.getAccountHolderName());
        map.put("accountNumber", s.getAccountNumber());
        map.put("ifscCode", s.getIfscCode());
        map.put("contactName", s.getContactName());
        map.put("contactDesignation", s.getContactDesignation());
        map.put("contactEmail", s.getContactEmail());
        map.put("contactPhone", s.getContactPhone());
        map.put("paymentTerms", s.getPaymentTerms());
        map.put("products", s.getProducts() != null ? new ArrayList<>(s.getProducts()) : null);
        return map;
    }

    /** SUP-0001 style code, based on total row count so far - simple, readable, and unique enough for
     * a single-tenant deployment (it's cosmetic, supplierCode isn't used as a lookup key anywhere). */
    private String generateCode(Supplier saved) {
        long ordinal = supplierRepository.count();
        return String.format("SUP-%04d", ordinal);
    }
}