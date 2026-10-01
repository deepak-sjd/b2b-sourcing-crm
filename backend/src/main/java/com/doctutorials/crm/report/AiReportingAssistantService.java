package com.doctutorials.crm.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Powers the "AI Reporting Assistant" tab from the screenshots
 * ("Break down leads by stage.", "Which counselor has the highest conversion rate?", ...).
 *
 * Deliberately NOT "pipe the question straight into an LLM with raw SQL access" -
 * that pattern is a prompt-injection / data-exfiltration risk in a real CRM with
 * PII. Instead this is an intent router over a small, fixed set of safe report
 * queries. Swap the keyword matching below for a proper NLU/embedding classifier
 * later without touching the underlying report calls.
 */
@Service
@RequiredArgsConstructor
public class AiReportingAssistantService {

    private final ReportService reportService;

    public record AiAnswer(String narrative, Object data) {}

    public AiAnswer answer(String question) {
        String q = question == null ? "" : question.toLowerCase();

        if (q.contains("stage")) {
            Map<String, Long> data = reportService.runLeadReport(ReportService.GroupBy.STAGE);
            return new AiAnswer(summarizeCounts("Leads by stage", data), data);
        }

        if (q.contains("source")) {
            Map<String, Long> data = reportService.runLeadReport(ReportService.GroupBy.SOURCE);
            return new AiAnswer(summarizeCounts("Leads by source", data), data);
        }

        if (q.contains("conversion") || q.contains("counselor") || q.contains("performer")) {
            List<ReportService.CounselorPerformance> data = reportService.counselorPerformance();
            String top = data.stream()
                    .max(Comparator())
                    .map(c -> c.ownerName() + " (" + String.format("%.1f", c.conversionRatePercent()) + "% conversion)")
                    .orElse("No counselor data yet");
            return new AiAnswer("Top performer: " + top, data);
        }

        // Fallback: give the overall pipeline snapshot rather than a dead-end "I don't understand".
        Map<String, Long> data = reportService.runLeadReport(ReportService.GroupBy.STAGE);
        return new AiAnswer(
                "I can currently answer questions about lead stage, lead source, and counselor performance. "
                        + "Here's your current pipeline by stage:",
                data);
    }

    private static java.util.Comparator<ReportService.CounselorPerformance> Comparator() {
        return java.util.Comparator.comparingDouble(ReportService.CounselorPerformance::conversionRatePercent);
    }

    private String summarizeCounts(String title, Map<String, Long> data) {
        long total = data.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder sb = new StringBuilder(title).append(" (total ").append(total).append("): ");
        data.forEach((k, v) -> sb.append(k).append("=").append(v).append("  "));
        return sb.toString().trim();
    }
}
