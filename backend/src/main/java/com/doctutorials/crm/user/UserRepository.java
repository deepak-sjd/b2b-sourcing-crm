package com.doctutorials.crm.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    // Used by LeadAutoAssignmentService to pick a round-robin owner for a
    // freshly created, unassigned lead. Unlike LeadRepository, this repo isn't
    // subject to lead-visibility scoping, so plain derived queries are fine here.
    List<User> findByRoleAndTeamIdAndActiveTrue(Role role, UUID teamId);
    List<User> findByRoleAndOrganizationIdAndActiveTrue(Role role, UUID organizationId);
}
