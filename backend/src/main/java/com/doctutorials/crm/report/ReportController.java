package com.doctutorials.crm.report;

import com.doctutorials.crm.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final AiReportingAssistantService aiReportingAssistantService;

    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<Map<String, Long>>> leadReport(
            @RequestParam(defaultValue = "STAGE") ReportService.GroupBy groupBy) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.runLeadReport(groupBy)));
    }

    @GetMapping("/counselor-performance")
    public ResponseEntity<ApiResponse<List<ReportService.CounselorPerformance>>> counselorPerformance() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.counselorPerformance()));
    }

    /** Backs the Dashboard KPI row (total/open/won/lost/hot leads, open pipeline value, avg score). */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<ReportService.DashboardSummary>> dashboardSummary() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.dashboardSummary()));
    }

    /** PRD §10 Sales Funnel: count + value per stage and per owner. */
    @GetMapping("/funnel")
    public ResponseEntity<ApiResponse<ReportService.FunnelReport>> funnel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.funnel(from, to)));
    }

    /** PRD §14.2 Lost business analysis, grouped by reason and by reason-group. */
    @GetMapping("/lost-analysis")
    public ResponseEntity<ApiResponse<ReportService.LostAnalysis>> lostAnalysis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.lostAnalysis(from, to)));
    }

    /** PRD §5/§15 source-wise lead counts (new/active/won per source). */
    @GetMapping("/lead-sources")
    public ResponseEntity<ApiResponse<List<ReportService.SourceBucket>>> leadSources(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.leadSources(from, to)));
    }

    /** PRD §12/§15 supplier sourcing activity. */
    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<ReportService.SupplierReport>> suppliers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.suppliers(from, to)));
    }

    /** PRD §13/§15 quotation status breakdown. */
    @GetMapping("/quotations")
    public ResponseEntity<ApiResponse<ReportService.QuotationReport>> quotations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.quotations(from, to)));
    }

    /** PRD §15 team performance. Scope (who the rows are about) is enforced in
     *  ReportService.resolveTeamPerformanceScope, not here. */
    @GetMapping("/team-performance")
    public ResponseEntity<ApiResponse<List<ReportService.TeamPerformanceRow>>> teamPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.teamPerformance(from, to)));
    }

    /** PRD §15 "Leads and customers" overview area. */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ReportService.OverviewReport>> overview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.overview(from, to)));
    }

    /**
     * Matches the "AI Reporting Assistant" tab: free-text question in,
     * narrative + structured insight out. See AiReportingAssistantService
     * for how the question gets routed to a real query instead of a raw LLM
     * call over your whole database (safer, cheaper, and deterministic).
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AiReportingAssistantService.AiAnswer>> ask(@RequestBody AskRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiReportingAssistantService.answer(request.question())));
    }

    public record AskRequest(String question) {}
}