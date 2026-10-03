package com.doctutorials.crm.lead;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leads", indexes = {
        @Index(name = "idx_leads_stage", columnList = "stage"),
        @Index(name = "idx_leads_owner", columnList = "owner_id"),
        @Index(name = "idx_leads_next_follow_up", columnList = "nextFollowUpAt")
})
@Getter
@Setter
public class Lead extends BaseEntity {

    @NotBlank
    private String fullName;

    private String phone;
    private String email;
    private String city;

    /** What the lead is interested in - a course, a product, a service line; whatever
     *  this business sells. Free text for now; becomes a proper FK once Products/Catalog exists. */
    private String interest;

    private BigDecimal budget;
    private BigDecimal estimatedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStage stage = LeadStage.NEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    /** 0-100 lead score, recomputed by LeadScoringService whenever activity changes. */
    @Column(nullable = false)
    private int score = 0;

    @Enumerated(EnumType.STRING)
    private BuyingIntent buyingIntent = BuyingIntent.COLD;

    /** Model-estimated probability (0-100) that this lead converts, shown as "Convert Probability". */
    private Integer convertProbability;

    private String source;       // e.g. "Meta Ads", "Referral", "Organic"
    private String campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    private Instant lastTouchpointAt;
    private Instant nextFollowUpAt;

    @Column(length = 2000)
    private String aiSummary;
    private Instant aiSummaryGeneratedAt;

    /** Single short, actionable recommendation (e.g. "Call within 24h — high
     *  intent, no contact in 6 days"), regenerated alongside aiSummary. Kept as
     *  its own column rather than parsed out of aiSummary so the UI can render
     *  it as a distinct call-to-action chip. */
    @Column(name = "next_best_action", length = 300)
    private String nextBestAction;

    // --- Won/Lost closure (PRD §14.1/§14.2) ---
    //
    // Set together by LeadService.markWon/markLost, which are the only paths
    // allowed to move a lead into WON or LOST — see changeStage's guard.

    private Instant closedAt;

    /** What the deal actually closed at, distinct from the earlier estimatedValue
     *  guess made during the pipeline. Null unless stage is WON. */
    private BigDecimal finalValue;

    @Column(length = 1000)
    private String closureRemarks;

    @Enumerated(EnumType.STRING)
    private LostReason lostReason;

    @Column(length = 1000)
    private String lostComment;

    // --- Scrap lifecycle (PRD §14.3) ---
    //
    // "Scrap means archived. It does NOT mean permanently deleted." - the row
    // stays, everything on it stays, it just drops out of the normal pipeline
    // views (see LeadService.notScrapped()) until reactivated. This was
    // previously a bare `archived` boolean with no reason/date/reactivate path
    // and wasn't exposed to the frontend at all; this migration promotes it to
    // the PRD's full Scrap concept and renames it to match that language
    // throughout (see V8 migration).

    @Column(nullable = false)
    private boolean scrapped = false;

    @Enumerated(EnumType.STRING)
    private ScrapReason scrapReason;

    @Column(length = 1000)
    private String scrapRemarks;

    private Instant scrappedAt;

    /**
     * Days since last touchpoint - used by the "inactive for N days, drop-off risk" banner.
     * Not persisted; computed on read.
     */
    @Transient
    public long getDaysSinceLastTouchpoint() {
        if (lastTouchpointAt == null) return -1;
        return java.time.Duration.between(lastTouchpointAt, Instant.now()).toDays();
    }
}
