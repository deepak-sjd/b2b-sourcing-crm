package com.doctutorials.crm.report;

import com.doctutorials.crm.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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