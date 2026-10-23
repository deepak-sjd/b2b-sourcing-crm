package com.doctutorials.crm;

import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.team.Team;
import com.doctutorials.crm.team.TeamRepository;
import com.doctutorials.crm.tenant.TenantContext;
import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Cross-tenant access attempts against a real PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired OrganizationRepository organizations;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;

    private Organization newOrg(String prefix) {
        Organization org = new Organization();
        org.setName(prefix);
        org.setSlug(prefix + "-" + UUID.randomUUID());
        return organizations.save(org);
    }

    private Team newTeam(Organization org, String name) {
        return TenantContext.supply(org.getId(), () -> {
            Team team = new Team();
            team.setName(name);
            team.setOrganization(org);
            return teams.save(team);
        });
    }

    private User newUser(Organization org) {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail(UUID.randomUUID() + "@test.local");
        user.setPasswordHash("x");
        user.setRole(Role.COUNSELOR);
        user.setOrganization(org);
        return users.save(user);
    }

    @Test
    void teamsAreInvisibleToOtherTenants() {
        Organization a = newOrg("a");
        Organization b = newOrg("b");
        Team teamA = newTeam(a, "Team A");

        assertThat(TenantContext.supply(a.getId(), () -> teams.findById(teamA.getId()))).isPresent();
        assertThat(TenantContext.supply(b.getId(), () -> teams.findById(teamA.getId()))).isEmpty();
        assertThat(TenantContext.supply(b.getId(), () -> teams.findAll()))
                .extracting(Team::getName).doesNotContain("Team A");
    }

    @Test
    void noTenantContextSeesNothing() {
        newTeam(newOrg("c"), "Hidden");
        assertThat(teams.findAll()).isEmpty();
    }

    @Test
    void insertGetsTheCurrentTenantAutomatically() {
        Organization a = newOrg("d");
        assertThat(newTeam(a, "Auto").getOrganizationId()).isEqualTo(a.getId());
    }

    @Test
    void userLookupsAreTenantScoped() {
        Organization a = newOrg("e");
        Organization b = newOrg("f");
        User userA = newUser(a);

        assertThat(TenantContext.supply(a.getId(), () -> users.findById(userA.getId()))).isPresent();
        assertThat(TenantContext.supply(b.getId(), () -> users.findById(userA.getId()))).isEmpty();
        assertThat(TenantContext.supply(b.getId(), () -> users.findAll()))
                .extracting(User::getId).doesNotContain(userA.getId());
        assertThat(users.findUnscopedById(userA.getId())).isPresent(); // auth path only
    }

        @Test
    void existsAndDeleteRespectTenant() {
        Organization a = newOrg("g");
        Organization b = newOrg("h");
        Team teamA = newTeam(a, "Keep me");

        assertThat(TenantContext.supply(b.getId(), () -> teams.existsById(teamA.getId()))).isFalse();
        TenantContext.runAs(b.getId(), () -> teams.deleteById(teamA.getId()));
        assertThat(TenantContext.supply(a.getId(), () -> teams.findById(teamA.getId()))).isPresent();
    }
}