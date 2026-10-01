package com.doctutorials.crm.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * AI automation #1 — deliberately a simple, explainable rule set (exact GST
 * match, or a case-insensitive exact company-name match) rather than a fuzzy
 * similarity model. A false "possible duplicate" costs one extra click to
 * dismiss; a missed one just means the previous (no-check) behavior. A
 * silent false negative from a black-box similarity score would be worse
 * than not having this feature, so this stays deterministic and swappable
 * for a real fuzzy-matching service later without touching the caller.
 */
@Service
@RequiredArgsConstructor
public class CustomerDuplicateService {

    private final CustomerRepository customerRepository;

    public record DuplicateMatch(UUID id, String companyName, String matchedOn) {}

    public List<DuplicateMatch> findPossibleDuplicates(String companyName, String gstNo, UUID excludingId) {
        String normalizedName = companyName == null ? "" : companyName.trim();
        String normalizedGst = (gstNo == null || gstNo.isBlank()) ? null : gstNo.trim().toUpperCase();

        return customerRepository.findPossibleDuplicates(normalizedName, normalizedGst).stream()
                .filter(c -> excludingId == null || !c.getId().equals(excludingId))
                .map(c -> new DuplicateMatch(
                        c.getId(),
                        c.getCompanyName(),
                        normalizedGst != null && normalizedGst.equalsIgnoreCase(c.getGstNo())
                                ? "Same GST number"
                                : "Same company name"))
                .toList();
    }
}
