package com.doctutorials.crm.lead;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    // Deliberately no
    // findByOwnerId/findByStage/findByArchivedFalseAndLastTouchpointAtBefore
    // here. Every list/lookup query for Lead now goes through LeadService, which
    // always ANDs
    // in LeadAccessService's visibility Specification - a plain derived-query
    // method sitting
    // on this repository would be an unscoped shortcut waiting to be reached for
    // later. If you
    // need a new lead query, add it as a Specification in
    // LeadAccessService/LeadService, not
    // a new method here.
    //
    // Same reasoning killed the old countGroupedByStage/countGroupedBySource/
    // counselorPerformance @Query methods that used to live here - they aggregated
    // across ALL
    // leads with no scoping. ReportService now computes the same numbers in Java
    // over a
    // leadAccessService.leadVisibility()-scoped result set instead.
}
