package com.doctutorials.crm;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        @Autowired JdbcTemplate jdbc;

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

        @Test
    void everyConnectionCarriesTheCurrentTenant() {
        Organization a = newOrg("i");
        String inTenant = TenantContext.supply(a.getId(), () ->
                jdbc.queryForObject("select current_setting('app.tenant_id', true)", String.class));
        assertThat(inTenant).isEqualTo(a.getId().toString());

        String noTenant = jdbc.queryForObject("select current_setting('app.tenant_id', true)", String.class);
        assertThat(noTenant).isEqualTo(TenantContext.SYSTEM.toString());
    }

    @Test
    void postgresRowLevelSecurityHidesOtherTenantsFromTheAppRole() throws Exception {
        Organization a = newOrg("j");
        Organization b = newOrg("k");
        Team teamA = newTeam(a, "RLS probe");

        try (Connection admin = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement st = admin.createStatement()) {
            st.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rls_probe') THEN "
                    + "CREATE ROLE rls_probe LOGIN PASSWORD 'probe' NOSUPERUSER NOBYPASSRLS; END IF; END $$");
            st.execute("GRANT USAGE ON SCHEMA public TO rls_probe");
            st.execute("GRANT SELECT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO rls_probe");
        }

        try (Connection probe = DriverManager.getConnection(postgres.getJdbcUrl(), "rls_probe", "probe")) {
            assertThat(visibleTeams(probe, a.getId().toString(), teamA.getId())).isEqualTo(1);
            assertThat(visibleTeams(probe, b.getId().toString(), teamA.getId())).isZero();
            assertThat(visibleTeams(probe, "", teamA.getId())).isZero();
            assertThat(visibleTeams(probe, TenantContext.SYSTEM.toString(), teamA.getId())).isZero();

            setTenant(probe, b.getId().toString());
            try (PreparedStatement ps = probe.prepareStatement("update teams set description = 'hacked' where id = ?")) {
                ps.setObject(1, teamA.getId());
                assertThat(ps.executeUpdate()).isZero();
            }
            try (PreparedStatement ps = probe.prepareStatement("delete from teams where id = ?")) {
                ps.setObject(1, teamA.getId());
                assertThat(ps.executeUpdate()).isZero();
            }
            assertThat(visibleTeams(probe, a.getId().toString(), teamA.getId())).isEqualTo(1);
        }
    }

    private int visibleTeams(Connection connection, String tenant, UUID teamId) throws SQLException {
        setTenant(connection, tenant);
        try (PreparedStatement ps = connection.prepareStatement("select count(*) from teams where id = ?")) {
            ps.setObject(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void setTenant(Connection connection, String tenant) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("select set_config('app.tenant_id', ?, false)")) {
            ps.setString(1, tenant);
            ps.execute();
        }
    }
}