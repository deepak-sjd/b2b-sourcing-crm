package com.doctutorials.crm.report;

import com.doctutorials.crm.lead.BuyingIntent;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadAccessService;
import com.doctutorials.crm.lead.LeadRepository;
import com.doctutorials.crm.lead.LeadStage;
import com.doctutorials.crm.config.CacheConfig;
import com.doctutorials.crm.task.TaskRepository;
import com.doctutorials.crm.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs the "Custom Reports & Analytics" screen: Data Source selector
 * (Leads Pipeline / Tasks & Follow-ups) x Group By (Stage / Source / City).
 *
 * Every number here is computed from a visibility-scoped lead list (via
 * LeadAccessService), so a MANAGER's "Custom Reports" only reflects their
 * own team and a COUNSELOR's only reflects their own leads - same rule as
 * the Leads screen itself, applied here in Java over the scoped result set
 * rather than as raw SQL aggregates, so there's exactly one place
 * (LeadAccessService) that defines who sees what.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final LeadAccessService leadAccessService;

    public enum DataSource {
        LEADS_PIPELINE, TASKS_FOLLOW_UPS, ACTIVITY_LOG
    }

    public enum GroupBy {
        NONE, STAGE, SOURCE, CITY
    }

    // Keyed on both groupBy AND the caller's id (not just groupBy) - results are
    // visibility-scoped per LeadAccessService, so caching only on groupBy would
    // serve one user's scoped numbers to a completely different user/team.
    // 30s TTL (see CacheConfig) trades a little staleness for not re-scanning
    // every visible lead on every dashboard/report paint.
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'lead-report:' + #groupBy + ':' + @currentUserService.get().id")
    public Map<String, Long> runLeadReport(GroupBy groupBy) {
        List<Lead> scoped = leadRepository.findAll(leadAccessService.leadVisibility());

        Map<String, Long> result = new LinkedHashMap<>();
        switch (groupBy) {
            case STAGE -> scoped.stream()
                    .collect(Collectors.groupingBy(l -> l.getStage().name(), Collectors.counting()))
                    .forEach(result::put);
            case SOURCE -> scoped.stream()
                    .collect(Collectors.groupingBy(l -> l.getSource() == null ? "Unknown" : l.getSource(),
                            Collectors.counting()))
                    .forEach(result::put);
            case CITY -> scoped.stream()
                    .collect(Collectors.groupingBy(l -> l.getCity() == null ? "Unknown" : l.getCity(),
                            Collectors.counting()))
                    .forEach(result::put);
            case NONE -> result.put("Total Leads", (long) scoped.size());
        }
        return result;
    }

    // Same per-caller keying reasoning as runLeadReport above.
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE, key = "'counselor-perf:' + @currentUserService.get().id")
    public List<CounselorPerformance> counselorPerformance() {
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and((root, query, cb) -> cb.isFalse(root.get("archived")))
                .and((root, query, cb) -> root.get("owner").isNotNull());

        Map<UUID, List<Lead>> byOwner = leadRepository.findAll(spec).stream()
                .collect(Collectors.groupingBy(l -> l.getOwner().getId()));

        return byOwner.values().stream()
                .map(leads -> {
                    User owner = leads.get(0).getOwner();
                    long assigned = leads.size();
                    long converted = leads.stream().filter(l -> l.getStage() == LeadStage.WON).count();
                    double rate = assigned == 0 ? 0.0 : (converted * 100.0 / assigned);
                    return new CounselorPerformance(owner.getId(), owner.getFullName(), assigned, converted, rate);
                })
                .sorted((a, b) -> Double.compare(b.conversionRatePercent(), a.conversionRatePercent()))
                .toList();
    }

    public record CounselorPerformance(
            UUID ownerId, String ownerName, long assigned, long converted, double conversionRatePercent) {
    }

    /**
     * Backs the Dashboard's KPI row. Deliberately a single aggregate endpoint
     * (one scoped fetch, computed in Java) rather than five separate ones -
     * same visibility-scoping reasoning as counselorPerformance above, and it
     * means the dashboard doesn't issue a handful of near-identical queries
     * on every paint.
     */
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE, key = "'dashboard-summary:' + @currentUserService.get().id")
    public DashboardSummary dashboardSummary() {
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and((root, query, cb) -> cb.isFalse(root.get("archived")));
        List<Lead> scoped = leadRepository.findAll(spec);

        long total = scoped.size();
        long won = scoped.stream().filter(l -> l.getStage() == LeadStage.WON).count();
        long lost = scoped.stream().filter(l -> l.getStage() == LeadStage.LOST).count();
        long hot = scoped.stream().filter(l -> l.getBuyingIntent() == BuyingIntent.HOT).count();

        BigDecimal openPipelineValue = scoped.stream()
                .filter(l -> l.getStage() != LeadStage.WON && l.getStage() != LeadStage.LOST)
                .map(l -> l.getEstimatedValue() != null ? l.getEstimatedValue()
                        : (l.getBudget() != null ? l.getBudget() : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double avgScore = scoped.isEmpty() ? 0.0 : scoped.stream().mapToInt(Lead::getScore).average().orElse(0.0);

        return new DashboardSummary(total, total - won - lost, won, lost, hot, openPipelineValue,
                Math.round(avgScore * 10) / 10.0);
    }

    public record DashboardSummary(
            long totalLeads,
            long openLeads,
            long wonLeads,
            long lostLeads,
            long hotLeads,
            BigDecimal openPipelineValue,
            double avgScore) {
    }
}