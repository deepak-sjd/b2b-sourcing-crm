package com.doctutorials.crm.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // Tenant-scoped overrides: every existing findById/findAll caller in the app is now
    // limited to the caller's own organization, with no edits at the call sites.
    @Override
    @Query("select u from User u where u.id = :id "
            + "and u.organization.id = :#{T(com.doctutorials.crm.tenant.TenantContext).currentOrSystem()}")
    Optional<User> findById(@Param("id") UUID id);

    @Override
    @Query("select u from User u "
            + "where u.organization.id = :#{T(com.doctutorials.crm.tenant.TenantContext).currentOrSystem()}")
    List<User> findAll();

    /** Authentication only (JwtAuthFilter): the tenant is not known until the user is loaded. */
    @Query("select u from User u join fetch u.organization where u.id = :id")
    Optional<User> findUnscopedById(@Param("id") UUID id);

    // Login and forgot-password look users up by email before any tenant is known.
    // Email stays globally unique (idx_users_email), so this is unambiguous.
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // Used by LeadAutoAssignmentService to pick a round-robin owner for a
    // freshly created, unassigned lead.
    List<User> findByRoleAndTeamIdAndActiveTrue(Role role, UUID teamId);

    List<User> findByRoleAndOrganizationIdAndActiveTrue(Role role, UUID organizationId);
}