package com.doctutorials.crm.report;

import com.doctutorials.crm.activity.Activity;
import com.doctutorials.crm.activity.ActivityAccessService;
import com.doctutorials.crm.activity.ActivityStatus;
import com.doctutorials.crm.activity.ActivityType;
import com.doctutorials.crm.config.CacheConfig;
import com.doctutorials.crm.customer.Customer;
import com.doctutorials.crm.customer.CustomerAccessService;
import com.doctutorials.crm.customer.CustomerRepository;
import com.doctutorials.crm.lead.BuyingIntent;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.LeadAccessService;
import com.doctutorials.crm.lead.LeadRepository;
import com.doctutorials.crm.lead.LeadStage;
import com.doctutorials.crm.lead.LostReason;
import com.doctutorials.crm.quotation.Quotation;
import com.doctutorials.crm.quotation.QuotationAccessService;
import com.doctutorials.crm.quotation.QuotationRepository;
import com.doctutorials.crm.quotation.QuotationStatus;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.supplier.Supplier;
import com.doctutorials.crm.supplier.SupplierRepository;
import com.doctutorials.crm.supplier.SupplierStatus;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiry;
import com.doctutorials.crm.supplier.enquiry.SupplierEnquiryReply;
import com.doctutorials.crm.supplier.enquiry.SupplierReplyStatus;
import com.doctutorials.crm.task.Task;
import com.doctutorials.crm.task.TaskRepository;
import com.doctutorials.crm.task.TaskStatus;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Backs both the original "Custom Reports & Analytics" screen (runLeadReport
 * / counselorPerformance / dashboardSummary - unchanged below, PRD-agnostic)
 * and PRD §15's Dashboard & Reports area (funnel / lostAnalysis / leadSources
 * / suppliers / quotations / teamPerformance / overview - new).
 *
 * The three original methods compute their numbers in Java over a
 * leadAccessService.leadVisibility()-scoped findAll() - see LeadRepository's
 * class comment for why that was a deliberate choice at the time. Everything
 * new in this file does the opposite on purpose: real GROUP BY/SUM queries
 * via EntityManager, because PRD §15 asks for this to stay correct at a
 * scale (50k+ leads/quotations/activities) where pulling every row into Java
 * just to sum it stops being a 30-second-stale-cache problem and becomes a
 * memory problem. Same visibility rule everywhere, just enforced as SQL
 * predicates instead of a Java filter.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final LeadAccessService leadAccessService;
    private final ActivityAccessService activityAccessService;
    private final QuotationAccessService quotationAccessService;
    private final CustomerAccessService customerAccessService;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final EntityManager em;

    public enum DataSource {
        LEADS_PIPELINE, TASKS_FOLLOW_UPS, ACTIVITY_LOG
    }

    public enum GroupBy {
        NONE, STAGE, SOURCE, CITY
    }

    // ================================================================
    // Original Report Builder endpoints - unchanged behavior/signatures.
    // ================================================================

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

    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE, key = "'counselor-perf:' + @currentUserService.get().id")
    public List<CounselorPerformance> counselorPerformance() {
        // Was root.get("archived") - stale: that column was renamed to
        // "scrapped" when Lead's archive flag was promoted to the full PRD
        // §14.3 Scrap concept (V15 migration). Left unnoticed here, this threw
        // an IllegalArgumentException on every call.
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and((root, query, cb) -> cb.isFalse(root.get("scrapped")))
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

    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE, key = "'dashboard-summary:' + @currentUserService.get().id")
    public DashboardSummary dashboardSummary() {
        // Same stale-column fix as counselorPerformance above.
        Specification<Lead> spec = Specification.where(leadAccessService.leadVisibility())
                .and((root, query, cb) -> cb.isFalse(root.get("scrapped")));
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

    // ================================================================
    // Date-range handling - shared by every PRD §15 endpoint below.
    // Package-private (not private) so ReportServiceTest can verify the
    // default-to-last-30-days behavior directly, with no DB involved.
    // ================================================================

    static LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(30);
        return new LocalDate[] { effectiveFrom, effectiveTo };
    }

    private static Instant startOf(LocalDate d) {
        return d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Exclusive upper bound, so "to" is treated as including that whole day. */
    private static Instant endOf(LocalDate d) {
        return d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** Specification.toPredicate(...), null-safe - SUPER_ADMIN/ADMIN's Specifications
     *  are literally null ("no restriction"), not a Specification that always matches. */
    private static <T> Predicate visibilityPredicate(Specification<T> spec, Root<T> root,
            CriteriaQuery<?> query, CriteriaBuilder cb) {
        return spec == null ? cb.conjunction() : spec.toPredicate(root, query, cb);
    }

    /**
     * SupplierEnquiry/SupplierEnquiryReply have no *AccessService of their own
     * (see SupplierEnquiryService's class comment - visibility is only ever
     * checked service-side via the parent lead). That's fine for the normal
     * get/list flows there, but an aggregate GROUP BY query here needs the
     * same rule expressed as a predicate against a *joined* lead path, and
     * Specification<Lead>.toPredicate(...) requires a real Root<Lead>, which
     * a Join<?,Lead> is not. Small deliberate duplication of
     * LeadAccessService's three-way rule rather than a type it doesn't fit -
     * keep this in sync with LeadAccessService.leadVisibility() if that rule
     * ever changes.
     */
    private Predicate leadOwnerVisibility(Path<Lead> leadPath, CriteriaBuilder cb) {
        User user = currentUserService.get();
        return switch (user.getRole()) {
            case SUPER_ADMIN, ADMIN -> cb.conjunction();
            case MANAGER -> {
                UUID teamId = user.getTeam() != null ? user.getTeam().getId() : null;
                yield teamId == null ? cb.disjunction()
                        : cb.equal(leadPath.get("owner").get("team").get("id"), teamId);
            }
            case COUNSELOR, VIEWER -> cb.equal(leadPath.get("owner").get("id"), user.getId());
        };
    }

    private static Expression<BigDecimal> leadValueExpression(CriteriaBuilder cb, Root<Lead> root) {
        return cb.coalesce(root.<BigDecimal>get("finalValue"),
                cb.coalesce(root.<BigDecimal>get("estimatedValue"),
                        cb.coalesce(root.<BigDecimal>get("budget"), BigDecimal.ZERO)));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    // ================================================================
    // PRD §15 / §14.2 / §5 - new aggregate report endpoints.
    // ================================================================

    public record FunnelStageBucket(String stage, long count, BigDecimal value) {
    }

    public record FunnelOwnerBucket(UUID ownerId, String ownerName, long count, BigDecimal value) {
    }

    public record FunnelReport(List<FunnelStageBucket> byStage, List<FunnelOwnerBucket> byOwner) {
    }

    /** PRD §10 Sales Funnel: count + value per stage, and per owner, within the window. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'funnel:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public FunnelReport funnel(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> stageQuery = cb.createTupleQuery();
        Root<Lead> stageRoot = stageQuery.from(Lead.class);
        Predicate stagePredicate = cb.and(
                visibilityPredicate(leadAccessService.leadVisibility(), stageRoot, stageQuery, cb),
                cb.between(stageRoot.get("createdAt"), start, end));
        stageQuery.multiselect(stageRoot.get("stage"), cb.count(stageRoot), cb.sum(leadValueExpression(cb, stageRoot)))
                .where(stagePredicate)
                .groupBy(stageRoot.get("stage"));
        List<FunnelStageBucket> byStage = em.createQuery(stageQuery).getResultList().stream()
                .map(t -> new FunnelStageBucket(((LeadStage) t.get(0)).name(), t.get(1, Long.class),
                        nz(t.get(2, BigDecimal.class))))
                .toList();

        CriteriaQuery<Tuple> ownerQuery = cb.createTupleQuery();
        Root<Lead> ownerRoot = ownerQuery.from(Lead.class);
        Join<Lead, User> ownerJoin = ownerRoot.join("owner", JoinType.LEFT);
        Predicate ownerPredicate = cb.and(
                visibilityPredicate(leadAccessService.leadVisibility(), ownerRoot, ownerQuery, cb),
                cb.between(ownerRoot.get("createdAt"), start, end));
        ownerQuery.multiselect(ownerJoin.get("id"), ownerJoin.get("fullName"), cb.count(ownerRoot),
                        cb.sum(leadValueExpression(cb, ownerRoot)))
                .where(ownerPredicate)
                .groupBy(ownerJoin.get("id"), ownerJoin.get("fullName"));
        List<FunnelOwnerBucket> byOwner = em.createQuery(ownerQuery).getResultList().stream()
                .map(t -> new FunnelOwnerBucket(t.get(0, UUID.class),
                        t.get(1, String.class) != null ? t.get(1, String.class) : "Unassigned",
                        t.get(2, Long.class), nz(t.get(3, BigDecimal.class))))
                .toList();

        return new FunnelReport(byStage, byOwner);
    }

    public record LostReasonBucket(String reason, long count, BigDecimal value) {
    }

    public record LostGroupBucket(String group, long count, BigDecimal value) {
    }

    public record LostAnalysis(long totalLostCount, BigDecimal totalLostValue,
            List<LostReasonBucket> byReason, List<LostGroupBucket> byGroup) {
    }

    /**
     * PRD §14.2: how the 12-value LostReason taxonomy rolls up into the 5
     * groups the Dashboard's "Lost business analysis" area names ("price-related
     * losses, supplier pricing issues, customer response issues, internal
     * delays, other"). The PRD doesn't spell out the mapping itself - this is
     * my judgment call, kept in one place so it's easy to re-balance later.
     */
    static String reasonGroup(LostReason reason) {
        return switch (reason) {
            case PRICE_TOO_HIGH -> "PRICE_RELATED";
            case SUPPLIER_PRICE_TOO_HIGH -> "SUPPLIER_PRICING";
            case CUSTOMER_STOPPED_RESPONDING, LATE_RESPONSE, CUSTOMER_NOT_INTERESTED -> "CUSTOMER_RESPONSE";
            case DELAY_FROM_OUR_SIDE, INCORRECT_REQUIREMENT -> "INTERNAL_DELAY";
            case CUSTOMER_SELECTED_ANOTHER_SUPPLIER, REQUIREMENT_CANCELLED, REQUIREMENT_POSTPONED,
                    REQUIREMENT_CHANGED, OTHER -> "OTHER";
        };
    }

    private static final List<String> LOST_GROUPS =
            List.of("PRICE_RELATED", "SUPPLIER_PRICING", "CUSTOMER_RESPONSE", "INTERNAL_DELAY", "OTHER");

    /** PRD §14.2 Lost business analysis, scoped + windowed by when the lead actually closed. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'lost-analysis:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public LostAnalysis lostAnalysis(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Lead> root = q.from(Lead.class);
        Expression<BigDecimal> value = leadValueExpression(cb, root);
        Predicate predicate = cb.and(
                visibilityPredicate(leadAccessService.leadVisibility(), root, q, cb),
                cb.equal(root.get("stage"), LeadStage.LOST),
                cb.between(root.get("closedAt"), start, end));
        q.multiselect(root.get("lostReason"), cb.count(root), cb.sum(value))
                .where(predicate)
                .groupBy(root.get("lostReason"));

        List<LostReasonBucket> byReason = em.createQuery(q).getResultList().stream()
                .map(t -> new LostReasonBucket(
                        t.get(0) == null ? "OTHER" : ((LostReason) t.get(0)).name(),
                        t.get(1, Long.class),
                        nz(t.get(2, BigDecimal.class))))
                .toList();

        long totalCount = byReason.stream().mapToLong(LostReasonBucket::count).sum();
        BigDecimal totalValue = byReason.stream().map(LostReasonBucket::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> groupCounts = new LinkedHashMap<>();
        Map<String, BigDecimal> groupValues = new LinkedHashMap<>();
        LOST_GROUPS.forEach(g -> {
            groupCounts.put(g, 0L);
            groupValues.put(g, BigDecimal.ZERO);
        });
        for (LostReasonBucket bucket : byReason) {
            String group = reasonGroup(LostReason.valueOf(bucket.reason()));
            groupCounts.merge(group, bucket.count(), Long::sum);
            groupValues.merge(group, bucket.value(), BigDecimal::add);
        }
        List<LostGroupBucket> byGroup = LOST_GROUPS.stream()
                .map(g -> new LostGroupBucket(g, groupCounts.get(g), groupValues.get(g)))
                .toList();

        return new LostAnalysis(totalCount, totalValue, byReason, byGroup);
    }

    public record SourceBucket(String source, long total, long newCount, long active, long won) {
    }

    /** PRD §15 "source-wise customer count". Grouped case-insensitively - "Meta Ads"
     *  and "meta ads" are the same source, just typed differently by whoever logged it. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'lead-sources:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public List<SourceBucket> leadSources(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Lead> root = q.from(Lead.class);
        Expression<String> sourceKey = cb.lower(cb.coalesce(root.<String>get("source"), "Unknown"));
        Expression<Long> newCount = cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("stage"), LeadStage.NEW), 1L).otherwise(0L));
        Expression<Long> activeCount = cb.sum(cb.<Long>selectCase()
                .when(cb.and(cb.notEqual(root.get("stage"), LeadStage.WON),
                        cb.notEqual(root.get("stage"), LeadStage.LOST),
                        cb.isFalse(root.get("scrapped"))), 1L)
                .otherwise(0L));
        Expression<Long> wonCount = cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("stage"), LeadStage.WON), 1L).otherwise(0L));

        Predicate predicate = cb.and(
                visibilityPredicate(leadAccessService.leadVisibility(), root, q, cb),
                cb.between(root.get("createdAt"), start, end));

        q.multiselect(sourceKey, cb.count(root), newCount, activeCount, wonCount)
                .where(predicate)
                .groupBy(sourceKey)
                .orderBy(cb.desc(cb.count(root)));

        return em.createQuery(q).getResultList().stream()
                .map(t -> new SourceBucket(capitalize(t.get(0, String.class)), t.get(1, Long.class),
                        nz(t.get(2, Long.class)), nz(t.get(3, Long.class)), nz(t.get(4, Long.class))))
                .toList();
    }

    /** Display label only - grouping already folded casing together, so this just
     *  picks a consistent presentation ("meta ads" -> "Meta ads"), not the exact
     *  original string (several different castings may have collapsed into one row). */
    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public record SupplierBucket(UUID supplierId, String supplierName, long enquiries, long replies, long chosen) {
    }

    public record SupplierReport(long activeSuppliers, long enquiriesSent, long pendingReplies,
            long repliesReceived, List<SupplierBucket> bySupplier) {
    }

    /** PRD §12/§15 supplier sourcing stats. Supplier master data itself is org-wide
     *  (no owner/visibility concept - see Supplier's class comment); the enquiry and
     *  reply activity around it is scoped by the lead that raised the enquiry. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'suppliers:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public SupplierReport suppliers(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        long activeSuppliers = supplierRepository.count((root, query, cb2) -> cb2.and(
                cb2.isFalse(root.get("archived")),
                cb2.notEqual(root.get("status"), SupplierStatus.BLACKLISTED)));

        CriteriaQuery<Long> enqQuery = cb.createQuery(Long.class);
        Root<SupplierEnquiry> enqRoot = enqQuery.from(SupplierEnquiry.class);
        Join<SupplierEnquiry, Lead> enqLead = enqRoot.join("lead");
        enqQuery.select(cb.count(enqRoot)).where(cb.and(
                leadOwnerVisibility(enqLead, cb),
                cb.between(enqRoot.get("createdAt"), start, end)));
        long enquiriesSent = em.createQuery(enqQuery).getSingleResult();

        CriteriaQuery<Tuple> replyQuery = cb.createTupleQuery();
        Root<SupplierEnquiryReply> replyRoot = replyQuery.from(SupplierEnquiryReply.class);
        Join<SupplierEnquiryReply, SupplierEnquiry> replyEnquiry = replyRoot.join("enquiry");
        Join<SupplierEnquiry, Lead> replyLead = replyEnquiry.join("lead");
        Join<SupplierEnquiryReply, Supplier> replySupplier = replyRoot.join("supplier");

        Predicate replyPredicate = cb.and(
                leadOwnerVisibility(replyLead, cb),
                cb.between(replyRoot.get("createdAt"), start, end));

        Expression<Long> repliedCase = cb.<Long>selectCase()
                .when(cb.equal(replyRoot.get("status"), SupplierReplyStatus.REPLIED), 1L).otherwise(0L);
        Expression<Long> pendingCase = cb.<Long>selectCase()
                .when(cb.equal(replyRoot.get("status"), SupplierReplyStatus.PENDING), 1L).otherwise(0L);
        Expression<Long> chosenCase = cb.<Long>selectCase()
                .when(cb.isTrue(replyRoot.get("chosen")), 1L).otherwise(0L);

        replyQuery.multiselect(replySupplier.get("id"), replySupplier.get("companyName"),
                        cb.count(replyRoot), cb.sum(repliedCase), cb.sum(pendingCase), cb.sum(chosenCase))
                .where(replyPredicate)
                .groupBy(replySupplier.get("id"), replySupplier.get("companyName"))
                .orderBy(cb.desc(cb.count(replyRoot)));

        List<Tuple> replyRows = em.createQuery(replyQuery).getResultList();
        List<SupplierBucket> bySupplier = replyRows.stream()
                .map(t -> new SupplierBucket(t.get(0, UUID.class), t.get(1, String.class),
                        t.get(2, Long.class), nz(t.get(3, Long.class)), nz(t.get(5, Long.class))))
                .toList();
        long repliesReceived = replyRows.stream().mapToLong(t -> nz(t.get(3, Long.class))).sum();
        long pendingReplies = replyRows.stream().mapToLong(t -> nz(t.get(4, Long.class))).sum();

        return new SupplierReport(activeSuppliers, enquiriesSent, pendingReplies, repliesReceived, bySupplier);
    }

    public record QuotationReport(
            long generated, long pending, long accepted, BigDecimal acceptedValue, long rejected, long expired) {
    }

    private static final List<QuotationStatus> PENDING_QUOTATION_STATUSES = List.of(
            QuotationStatus.DRAFT, QuotationStatus.UNDER_REVIEW, QuotationStatus.SENT,
            QuotationStatus.VIEWED, QuotationStatus.NEGOTIATION);

    /** PRD §13 quotation status breakdown, windowed by quotationDate. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'quotations:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public QuotationReport quotations(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<Quotation> root = q.from(Quotation.class);
        Predicate predicate = cb.and(
                visibilityPredicate(quotationAccessService.quotationVisibility(), root, q, cb),
                cb.between(root.get("quotationDate"), range[0], range[1]));

        Expression<Long> pendingCount = cb.sum(cb.<Long>selectCase()
                .when(root.get("status").in(PENDING_QUOTATION_STATUSES), 1L).otherwise(0L));
        Expression<Long> acceptedCount = cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("status"), QuotationStatus.ACCEPTED), 1L).otherwise(0L));
        Expression<BigDecimal> acceptedValue = cb.sum(cb.<BigDecimal>selectCase()
                .when(cb.equal(root.get("status"), QuotationStatus.ACCEPTED), root.<BigDecimal>get("grandTotal"))
                .otherwise(BigDecimal.ZERO));
        Expression<Long> rejectedCount = cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("status"), QuotationStatus.REJECTED), 1L).otherwise(0L));
        Expression<Long> expiredCount = cb.sum(cb.<Long>selectCase()
                .when(cb.equal(root.get("status"), QuotationStatus.EXPIRED), 1L).otherwise(0L));

        q.multiselect(cb.count(root), pendingCount, acceptedCount, acceptedValue, rejectedCount, expiredCount)
                .where(predicate);

        Tuple row = em.createQuery(q).getSingleResult();
        return new QuotationReport(nz(row.get(0, Long.class)), nz(row.get(1, Long.class)),
                nz(row.get(2, Long.class)), nz(row.get(3, BigDecimal.class)),
                nz(row.get(4, Long.class)), nz(row.get(5, Long.class)));
    }

    public record TeamPerformanceRow(
            UUID userId, String userName,
            long assignedLeads, long callsCompleted, long meetingsCompleted,
            long followUpsCompleted, long pendingTasks, long quotationsHandled,
            long won, long lost) {
    }

    /**
     * Scope is who the numbers are ABOUT (a user set), not the usual "which rows
     * can I see" rule - that's what task #1 explicitly specifies (ADMIN/SUPER_ADMIN
     * all, MANAGER own team, COUNSELOR/VIEWER only themselves), so it's resolved as
     * its own thing here rather than reusing LeadAccessService/ActivityAccessService
     * (which scope by data ownership chains, not a flat user list).
     * Package-private + no DB call on the COUNSELOR/VIEWER/empty-team branches so
     * ReportServiceTest can exercise the branching without mocking EntityManager.
     */
    List<User> resolveTeamPerformanceScope(User current) {
        return switch (current.getRole()) {
            case SUPER_ADMIN, ADMIN -> userRepository.findAll();
            case MANAGER -> {
                if (current.getTeam() == null) {
                    yield List.of(); // fail closed, same reasoning as LeadAccessService
                }
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<User> q = cb.createQuery(User.class);
                Root<User> root = q.from(User.class);
                q.select(root).where(cb.and(
                        cb.equal(root.get("team").get("id"), current.getTeam().getId()),
                        cb.isTrue(root.get("active"))));
                yield em.createQuery(q).getResultList();
            }
            case COUNSELOR, VIEWER -> List.of(current);
        };
    }

    private interface FilterFn<T> {
        Predicate apply(Root<T> root, CriteriaBuilder cb);
    }

    /** One GROUP BY query per metric, each pre-filtered to `userField IN scopeUserIds`
     *  (a LEFT JOIN so a row with no user on it is simply excluded by the IN, rather
     *  than needing special null handling the way funnel()'s owner breakdown does). */
    private <T> Map<UUID, Long> groupCountByUser(CriteriaBuilder cb, Class<T> entityClass, String userField,
            List<UUID> scopeUserIds, FilterFn<T> filter) {
        CriteriaQuery<Tuple> q = cb.createTupleQuery();
        Root<T> root = q.from(entityClass);
        Join<T, User> userJoin = root.join(userField, JoinType.LEFT);
        q.multiselect(userJoin.get("id"), cb.count(root))
                .where(cb.and(userJoin.get("id").in(scopeUserIds), filter.apply(root, cb)))
                .groupBy(userJoin.get("id"));
        return em.createQuery(q).getResultList().stream()
                .collect(Collectors.toMap(t -> t.get(0, UUID.class), t -> t.get(1, Long.class)));
    }

    /** PRD §15 Team performance. Hidden on the frontend for roles the product
     *  doesn't want seeing staff metrics (see INTEGRATION.md) - the backend scope
     *  above still applies regardless, so a direct API call is never unscoped. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'team-performance:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public List<TeamPerformanceRow> teamPerformance(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);

        List<User> scope = resolveTeamPerformanceScope(currentUserService.get());
        if (scope.isEmpty()) {
            return List.of();
        }
        List<UUID> userIds = scope.stream().map(User::getId).toList();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        Map<UUID, Long> assigned = groupCountByUser(cb, Lead.class, "owner", userIds,
                (root, c) -> c.between(root.get("createdAt"), start, end));
        Map<UUID, Long> won = groupCountByUser(cb, Lead.class, "owner", userIds,
                (root, c) -> c.and(c.equal(root.get("stage"), LeadStage.WON),
                        c.between(root.get("closedAt"), start, end)));
        Map<UUID, Long> lost = groupCountByUser(cb, Lead.class, "owner", userIds,
                (root, c) -> c.and(c.equal(root.get("stage"), LeadStage.LOST),
                        c.between(root.get("closedAt"), start, end)));
        Map<UUID, Long> calls = groupCountByUser(cb, Activity.class, "loggedBy", userIds,
                (root, c) -> c.and(c.equal(root.get("activityType"), ActivityType.CALL),
                        c.equal(root.get("status"), ActivityStatus.COMPLETED),
                        c.between(root.get("scheduledAt"), start, end)));
        Map<UUID, Long> meetings = groupCountByUser(cb, Activity.class, "loggedBy", userIds,
                (root, c) -> c.and(c.equal(root.get("activityType"), ActivityType.MEETING),
                        c.equal(root.get("status"), ActivityStatus.COMPLETED),
                        c.between(root.get("scheduledAt"), start, end)));
        Map<UUID, Long> followUps = groupCountByUser(cb, Task.class, "assignedTo", userIds,
                (root, c) -> c.and(c.equal(root.get("status"), TaskStatus.COMPLETED),
                        c.between(root.get("completedAt"), start, end)));
        // Deliberately NOT date-ranged: "pending" is a right-now count, not
        // something that happened within the selected window. Includes OVERDUE -
        // still outstanding work, just late, not a different bucket from "pending".
        Map<UUID, Long> pending = groupCountByUser(cb, Task.class, "assignedTo", userIds,
                (root, c) -> root.get("status")
                        .in(List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.OVERDUE)));
        Map<UUID, Long> quotationsHandled = groupCountByUser(cb, Quotation.class, "preparedBy", userIds,
                (root, c) -> c.between(root.get("quotationDate"), range[0], range[1]));

        return scope.stream()
                .map(u -> new TeamPerformanceRow(u.getId(), u.getFullName(),
                        nz(assigned.get(u.getId())), nz(calls.get(u.getId())), nz(meetings.get(u.getId())),
                        nz(followUps.get(u.getId())), nz(pending.get(u.getId())), nz(quotationsHandled.get(u.getId())),
                        nz(won.get(u.getId())), nz(lost.get(u.getId()))))
                .toList();
    }

    public record OverviewReport(
            long totalCustomers, long newCustomers, long activeCustomers,
            long leadsNeedingFollowUp, long scrapCount,
            long customersContacted, long customersQualified, long meetingsScheduled) {
    }

    /** PRD §15 "Leads and customers" area. Customer itself carries no pipeline
     *  stage (see Customer's class comment), so "customers contacted/qualified"
     *  maps onto the Lead funnel's CONTACTED/QUALIFIED stages - that's where the
     *  PRD's pre-conversion customer journey actually lives in this schema. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.REPORTS_CACHE,
            key = "'overview:' + #from + ':' + #to + ':' + @currentUserService.get().id")
    public OverviewReport overview(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Instant start = startOf(range[0]);
        Instant end = endOf(range[1]);

        long totalCustomers = customerRepository.count(customerAccessService.customerVisibility());
        long newCustomers = customerRepository.count(Specification.where(customerAccessService.customerVisibility())
                .and((root, q, cb) -> cb.between(root.get("createdAt"), start, end)));
        long activeCustomers = customerRepository.count(
                Specification.where(customerAccessService.customerVisibility())
                        .and((root, q, cb) -> cb.isTrue(root.get("active"))));

        long leadsNeedingFollowUp = leadRepository.count(Specification.where(leadAccessService.leadVisibility())
                .and((root, q, cb) -> cb.and(
                        cb.isFalse(root.get("scrapped")),
                        cb.not(root.get("stage").in(List.of(LeadStage.WON, LeadStage.LOST))),
                        cb.isNotNull(root.get("nextFollowUpAt")),
                        cb.lessThanOrEqualTo(root.get("nextFollowUpAt"), Instant.now()))));

        long scrapCount = leadRepository.count(Specification.where(leadAccessService.leadVisibility())
                .and((root, q, cb) -> cb.isTrue(root.get("scrapped"))));

        long contacted = leadRepository.count(Specification.where(leadAccessService.leadVisibility())
                .and((root, q, cb) -> cb.and(cb.equal(root.get("stage"), LeadStage.CONTACTED),
                        cb.between(root.get("createdAt"), start, end))));
        long qualified = leadRepository.count(Specification.where(leadAccessService.leadVisibility())
                .and((root, q, cb) -> cb.and(cb.equal(root.get("stage"), LeadStage.QUALIFIED),
                        cb.between(root.get("createdAt"), start, end))));

        CriteriaBuilder cb2 = em.getCriteriaBuilder();
        CriteriaQuery<Long> meetingQuery = cb2.createQuery(Long.class);
        Root<Activity> meetingRoot = meetingQuery.from(Activity.class);
        Predicate meetingPredicate = cb2.and(
                visibilityPredicate(activityAccessService.activityVisibility(), meetingRoot, meetingQuery, cb2),
                cb2.equal(meetingRoot.get("activityType"), ActivityType.MEETING),
                cb2.equal(meetingRoot.get("status"), ActivityStatus.SCHEDULED),
                cb2.between(meetingRoot.get("scheduledAt"), start, end));
        meetingQuery.select(cb2.count(meetingRoot)).where(meetingPredicate);
        long meetingsScheduled = em.createQuery(meetingQuery).getSingleResult();

        return new OverviewReport(totalCustomers, newCustomers, activeCustomers, leadsNeedingFollowUp, scrapCount,
                contacted, qualified, meetingsScheduled);
    }
}