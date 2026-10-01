package com.doctutorials.crm.supplier;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * "Possible duplicate" warnings shown on the supplier form before save -
 * this is the automation the client asked for so the same vendor doesn't
 * get entered twice under slightly different names by two different staff
 * members. It only ever WARNS (except an exact GST match, which is a hard
 * block in SupplierService - two GST numbers can't legitimately belong to
 * two different real companies) - a human still decides whether "Ravi
 * Traders" and "Ravi Traders Pvt Ltd" are the same vendor or not.
 */
@Service
@RequiredArgsConstructor
public class SupplierDuplicateService {

    private final SupplierRepository supplierRepository;

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]");

    public record DuplicateMatch(UUID id, String companyName, String reason, int confidencePercent) {}

    public List<DuplicateMatch> findPotentialDuplicates(String companyName, String gstNumber, String contactPhone,
            UUID excludeId) {
        List<DuplicateMatch> matches = new ArrayList<>();
        String normalizedTarget = normalize(companyName);
        String normalizedPhone = digitsOnly(contactPhone);

        List<Supplier> candidates = supplierRepository.findAll().stream()
                .filter(s -> !s.isArchived())
                .filter(s -> excludeId == null || !s.getId().equals(excludeId))
                .toList();

        for (Supplier candidate : candidates) {
            if (gstNumber != null && !gstNumber.isBlank()
                    && gstNumber.equalsIgnoreCase(candidate.getGstNumber())) {
                matches.add(new DuplicateMatch(candidate.getId(), candidate.getCompanyName(),
                        "Same GST number as \"" + candidate.getCompanyName() + "\"", 100));
                continue;
            }

            if (!normalizedPhone.isBlank() && normalizedPhone.equals(digitsOnly(candidate.getContactPhone()))) {
                matches.add(new DuplicateMatch(candidate.getId(), candidate.getCompanyName(),
                        "Same contact number as \"" + candidate.getCompanyName() + "\"", 90));
                continue;
            }

            if (!normalizedTarget.isBlank()) {
                String normalizedCandidate = normalize(candidate.getCompanyName());
                int similarity = similarityPercent(normalizedTarget, normalizedCandidate);
                if (similarity >= 85) {
                    matches.add(new DuplicateMatch(candidate.getId(), candidate.getCompanyName(),
                            "Name looks similar to \"" + candidate.getCompanyName() + "\"", similarity));
                }
            }
        }

        matches.sort((a, b) -> Integer.compare(b.confidencePercent(), a.confidencePercent()));
        return matches;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return NON_ALPHANUMERIC.matcher(value.toLowerCase()).replaceAll("");
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }

    /** Simple Levenshtein-distance-based similarity, expressed as 0-100. */
    private int similarityPercent(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0;
        int distance = levenshtein(a, b);
        int maxLen = Math.max(a.length(), b.length());
        return (int) Math.round((1.0 - (double) distance / maxLen) * 100);
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            System.arraycopy(curr, 0, prev, 0, curr.length);
        }
        return prev[b.length()];
    }
}
