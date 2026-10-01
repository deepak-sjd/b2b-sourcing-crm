package com.doctutorials.crm.requirement;

import com.doctutorials.crm.common.BaseEntity;
import com.doctutorials.crm.lead.Lead;
import com.doctutorials.crm.lead.Priority;
import com.doctutorials.crm.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "requirements", indexes = {
        @Index(name = "idx_requirements_lead_id", columnList = "lead_id"),
        @Index(name = "idx_requirements_status", columnList = "status"),
        @Index(name = "idx_requirements_assigned_to_id", columnList = "assigned_to_id"),
        @Index(name = "idx_requirements_delivery_date", columnList = "requiredDeliveryDate")
})
@Getter
@Setter
public class Requirement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    /** Free text until the Product/Service master (PRD section 21) exists; then it becomes an FK. */
    @NotBlank
    private String productName;

    @Column(length = 2000)
    private String description;

    private BigDecimal quantity;
    private String unit;
    private String expectedTimeline;
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    private LocalDate requiredDeliveryDate;

    @Column(length = 2000)
    private String additionalSpecifications;

    @Column(length = 2000)
    private String remarks;

    private boolean needsSupplier = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequirementStatus status = RequirementStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
