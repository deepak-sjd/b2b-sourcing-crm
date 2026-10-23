package com.doctutorials.crm.seed;

import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.team.Team;
import com.doctutorials.crm.team.TeamRepository;
import com.doctutorials.crm.tenant.TenantContext;
import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bootstraps a default Super Admin account on first startup, ONLY if the
 * users table is completely empty. Breaks the chicken-and-egg problem of
 * elevated roles only being grantable by an existing admin.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.seed.admin-email:admin@doctutorials.local}")
    private String adminEmail;

    @Value("${app.seed.admin-password:ChangeMe123!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("[seed] seeding disabled via app.seed.enabled=false");
            return;
        }

        if (userRepository.count() > 0) {
            log.info("[seed] users already exist, skipping seed");
            return;
        }

        Organization defaultOrg = organizationRepository.findBySlug("default")
                .orElseGet(() -> {
                    Organization org = new Organization();
                    org.setName("Default Organization");
                    org.setSlug("default");
                    return organizationRepository.save(org);
                });

        // Tenant must be set BEFORE the transaction opens (a session's tenant is fixed at open).
        TenantContext.runAs(defaultOrg.getId(), () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    Team defaultTeam = new Team();
                    defaultTeam.setName("Default Team");
                    defaultTeam.setDescription("Auto-created by DataSeeder");
                    defaultTeam.setOrganization(defaultOrg);
                    defaultTeam = teamRepository.save(defaultTeam);

                    User admin = new User();
                    admin.setFullName("Super Admin");
                    admin.setEmail(adminEmail.toLowerCase());
                    admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                    admin.setRole(Role.SUPER_ADMIN);
                    admin.setTeam(defaultTeam);
                    admin.setOrganization(defaultOrg);
                    userRepository.save(admin);
                }));

        log.warn("[seed] Created default Super Admin '{}' with a default password. "
                + "CHANGE THIS PASSWORD IMMEDIATELY in any real environment - "
                + "set app.seed.admin-password via CRM_SEED_ADMIN_PASSWORD, or "
                + "disable seeding entirely after first login via app.seed.enabled=false.",
                adminEmail);
    }
}