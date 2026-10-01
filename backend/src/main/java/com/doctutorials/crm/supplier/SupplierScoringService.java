package com.doctutorials.crm.supplier;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the 0-100 "profile strength" score shown as a bar on the supplier
 * card, plus a one-line note on what to add next. Deliberately simple, fully
 * deterministic weighted-checklist scoring (no external AI call) - same
 * philosophy as LeadScoringService: transparent and explainable beats a
 * black box for something a procurement admin will be asked to justify.
 */
@Service
public class SupplierScoringService {

    public void rescore(Supplier s) {
        int score = 0;
        List<String> missing = new ArrayList<>();

        if (s.getGstNumber() != null && !s.getGstNumber().isBlank()) {
            score += s.isGstVerified() ? 25 : 12;
            if (!s.isGstVerified()) missing.add("verify GSTIN");
        } else {
            missing.add("add a GST number");
        }

        boolean bankComplete = notBlank(s.getBankName()) && notBlank(s.getAccountNumber()) && notBlank(s.getIfscCode());
        if (bankComplete) {
            score += 20;
        } else {
            missing.add("complete bank details");
        }

        boolean contactComplete = notBlank(s.getContactPhone()) && notBlank(s.getContactEmail());
        if (contactComplete) {
            score += 20;
        } else {
            missing.add("add a contact phone and email");
        }

        boolean addressComplete = notBlank(s.getAddressLine1()) && notBlank(s.getCity())
                && notBlank(s.getState()) && notBlank(s.getPincode());
        if (addressComplete) {
            score += 15;
        } else {
            missing.add("complete the address");
        }

        if (s.getPaymentTerms() != null) {
            score += 10;
        } else {
            missing.add("set payment terms");
        }

        if (s.getProducts() != null && !s.getProducts().isEmpty()) {
            score += 10;
        } else {
            missing.add("list what they supply");
        }

        if (s.getCategory() != null && s.getCategory() != SupplierCategory.OTHER) {
            score += 0; // category is required at form level already; no separate credit needed beyond default
        }

        s.setTrustScore(Math.min(100, score));
        s.setScoreNote(missing.isEmpty()
                ? "Profile complete."
                : "To improve this score: " + String.join(", ", missing) + ".");
    }

    private boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }
}
