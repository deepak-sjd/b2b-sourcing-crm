package com.doctutorials.crm.lead.dto;

import java.util.List;

public record LeadImportResult(int totalRows, int created, int duplicates, int failed, List<RowIssue> issues) {

    public record RowIssue(int row, String name, String reason) {}
}