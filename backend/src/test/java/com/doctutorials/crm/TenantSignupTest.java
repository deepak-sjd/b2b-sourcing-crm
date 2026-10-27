package com.doctutorials.crm;

import com.doctutorials.crm.auth.TenantSignupService;
import com.doctutorials.crm.auth.dto.AuthResponse;
import com.doctutorials.crm.auth.dto.SignupRequest;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.team.Team;
import com.doctutorials.crm.team.TeamRepository;
import com.doctutorials.crm.tenant.TenantContext;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TenantSignupTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired TenantSignupService signup;
    @Autowired UserRepository users;
    @Autowired TeamRepository teams;
    @Autowired OrganizationRepository organizations;

    private SignupRequest req(String company, String email) {
        return new SignupRequest(company, "Owner", email, "password123");
    }

    private UUID orgOf(String email) {
        User user = users.findByEmailIgnoreCase(email).orElseThrow();
        return users.findUnscopedById(user.getId()).orElseThrow().getOrganization().getId();
    }

    @Test
    void signupCreatesAnIsolatedWorkspaceWithAnAdminOwner() {
        String e1 = UUID.randomUUID() + "@one.test";
        String e2 = UUID.randomUUID() + "@two.test";

        AuthResponse r1 = signup.signup(req("Acme Traders", e1));
        signup.signup(req("Acme Traders", e2)); // same company name still gets its own workspace

        assertThat(r1.role()).isEqualTo("ADMIN");
        UUID org1 = orgOf(e1);
        UUID org2 = orgOf(e2);
        assertThat(org1).isNotEqualTo(org2);

        List<Team> teams1 = TenantContext.supply(org1, () -> teams.findAll());
        List<Team> teams2 = TenantContext.supply(org2, () -> teams.findAll());
        assertThat(teams1).hasSize(1);
        assertThat(teams2).hasSize(1);
        assertThat(teams1.get(0).getId()).isNotEqualTo(teams2.get(0).getId());
    }

    @Test
    void duplicateEmailIsRejectedAndLeavesNoOrphanWorkspace() {
        String email = UUID.randomUUID() + "@dup.test";
        signup.signup(req("First Co", email));
        long before = organizations.count();

        assertThatThrownBy(() -> signup.signup(req("Second Co", email)))
                .isInstanceOf(BadRequestException.class);
        assertThat(organizations.count()).isEqualTo(before);
    }
}