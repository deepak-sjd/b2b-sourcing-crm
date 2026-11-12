package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.lead.dto.LeadImportResult;
import com.doctutorials.crm.lead.dto.LeadImportResult.RowIssue;
import com.doctutorials.crm.lead.dto.LeadRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Bulk lead upload (PRD §5.1): validate each row, skip duplicates (inside the file and against
 * existing leads), and create the rest through LeadService.create so scoring, auto-assignment,
 * tenant and audit rules all apply. Deliberately NOT @Transactional: each row saves on its own,
 * so one bad row never loses the others.
 */
@Service
@RequiredArgsConstructor
public class LeadImportService {

    private static final int MAX_ROWS = 1000;

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("name", "name"), Map.entry("fullname", "name"), Map.entry("leadname", "name"),
            Map.entry("contactname", "name"),
            Map.entry("phone", "phone"), Map.entry("mobile", "phone"), Map.entry("phonenumber", "phone"),
            Map.entry("mobilenumber", "phone"), Map.entry("contactnumber", "phone"),
            Map.entry("email", "email"), Map.entry("emailid", "email"), Map.entry("emailaddress", "email"),
            Map.entry("city", "city"), Map.entry("location", "city"),
            Map.entry("interest", "interest"), Map.entry("product", "interest"),
            Map.entry("requirement", "interest"),
            Map.entry("budget", "budget"), Map.entry("priority", "priority"),
            Map.entry("source", "source"), Map.entry("campaign", "campaign"));

    private final LeadService leadService;
    private final LeadRepository leadRepository;

    public LeadImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose a CSV file to upload");
        }
        List<List<String>> rows;
        try {
            rows = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BadRequestException("Couldn't read the file");
        }
        if (rows.size() < 2) {
            throw new BadRequestException("The file needs a header row and at least one lead");
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw new BadRequestException("Upload at most " + MAX_ROWS + " leads at a time");
        }

        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) {
            String key = ALIASES.get(header.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
            if (key != null) {
                col.putIfAbsent(key, i);
            }
        }
        if (!col.containsKey("name")) {
            throw new BadRequestException("The first row must contain a 'name' column");
        }

        Set<String> seen = new HashSet<>();
        List<RowIssue> issues = new ArrayList<>();
        int total = 0, created = 0, duplicates = 0, failed = 0;

        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            int line = i + 1;
            String name = cell(r, col, "name");
            String phone = cell(r, col, "phone");
            String email = cell(r, col, "email");
            if (name.isBlank() && phone.isBlank() && email.isBlank()) {
                continue; // blank line
            }
            total++;

            String problem = null;
            if (name.isBlank()) {
                problem = "Name is required";
            } else if (phone.isBlank() && email.isBlank()) {
                problem = "A phone number or email is required";
            } else if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                problem = "Email '" + email + "' is not valid";
            }

            BigDecimal budget = null;
            Priority priority = null;
            if (problem == null && !cell(r, col, "budget").isBlank()) {
                try {
                    budget = new BigDecimal(cell(r, col, "budget").replaceAll("[,₹\\s]", ""));
                } catch (NumberFormatException e) {
                    problem = "Budget '" + cell(r, col, "budget") + "' is not a number";
                }
            }
            if (problem == null && !cell(r, col, "priority").isBlank()) {
                try {
                    priority = Priority.valueOf(cell(r, col, "priority").toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    problem = "Priority must be LOW, MEDIUM, HIGH or URGENT";
                }
            }
            if (problem != null) {
                failed++;
                issues.add(new RowIssue(line, name, problem));
                continue;
            }

            String digits = phone.replaceAll("\\D", "");
            String emailKey = email.toLowerCase(Locale.ROOT);
            boolean duplicate = (!digits.isEmpty() && !seen.add("p:" + digits))
                    || (!emailKey.isEmpty() && !seen.add("e:" + emailKey))
                    || (!phone.isBlank() && (leadRepository.existsByPhone(phone)
                            || (!digits.equals(phone) && leadRepository.existsByPhone(digits))))
                    || (!email.isBlank() && leadRepository.existsByEmailIgnoreCase(email));
            if (duplicate) {
                duplicates++;
                issues.add(new RowIssue(line, name, "Duplicate - a lead with this phone or email already exists"));
                continue;
            }

            try {
                leadService.create(new LeadRequest(name, nullIfBlank(phone), nullIfBlank(email),
                        nullIfBlank(cell(r, col, "city")), nullIfBlank(cell(r, col, "interest")), budget, priority,
                        nullIfBlank(cell(r, col, "source")), nullIfBlank(cell(r, col, "campaign")), null));
                created++;
            } catch (RuntimeException e) {
                failed++;
                issues.add(new RowIssue(line, name, "Could not be saved: " + e.getMessage()));
            }
        }
        return new LeadImportResult(total, created, duplicates, failed, issues);
    }

    private static String cell(List<String> row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        return idx == null || idx >= row.size() ? "" : row.get(idx).trim();
    }

    private static String nullIfBlank(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /** Minimal RFC-4180 reader: quoted cells, escaped quotes, commas/newlines inside quotes, BOM, CRLF. */
    static List<List<String>> parseCsv(String text) {
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }
}