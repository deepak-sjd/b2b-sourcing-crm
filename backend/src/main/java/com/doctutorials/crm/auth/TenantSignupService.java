package com.doctutorials.crm.auth;

import com.doctutorials.crm.auth.dto.AuthResponse;
import com.doctutorials.crm.auth.dto.SignupRequest;
import com.doctutorials.crm.auth.token.RefreshTokenService;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.security.JwtUtil;
import com.doctutorials.crm.team.Team;
import com.doctutorials.crm.team.TeamRepository;
import com.doctutorials.crm.tenant.TenantContext;
import com.doctutorials.crm.user.Role;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.UUID;

/**
 * Creates a brand-new tenant: organization, a first team, and an ADMIN owner.
 * The tenant context must be set BEFORE the transaction opens (a session's tenant is
 * fixed at open), hence the TransactionTemplate inside TenantContext.supply.
 */
@Service
@RequiredArgsConstructor
public class TenantSignupService {

    private static final Logger log = LoggerFactory.getLogger(TenantSignupService.class);

    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PlatformTransactionManager transactionManager;

    public AuthResponse signup(SignupRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("A user with this email already exists");
        }

        Organization org = new Organization();
        org.setName(req.companyName().trim());
        org.setSlug(uniqueSlug(req.companyName()));
        final Organization savedOrg = organizationRepository.save(org);

        User admin;
        try {
            admin = TenantContext.supply(savedOrg.getId(), () ->
                    new TransactionTemplate(transactionManager).execute(status -> {
                        Team team = new Team();
                        team.setName("Main Team");
                        team.setDescription("Created at sign-up");
                        team.setOrganization(savedOrg);
                        team = teamRepository.save(team);

                        User user = new User();
                        user.setFullName(req.fullName().trim());
                        user.setEmail(email);
                        user.setPasswordHash(passwordEncoder.encode(req.password()));
                        user.setRole(Role.ADMIN);
                        user.setTeam(team);
                        user.setOrganization(savedOrg);
                        return userRepository.save(user);
                    }));
        } catch (RuntimeException e) {
            // Undo the half-created workspace so no empty organization is left behind.
            try {
                organizationRepository.deleteById(savedOrg.getId());
            } catch (RuntimeException cleanup) {
                log.warn("Could not clean up organization {} after failed sign-up", savedOrg.getId(), cleanup);
            }
            if (e instanceof DataIntegrityViolationException) {
                throw new BadRequestException("A user with this email already exists");
            }
            throw e;
        }

        String accessToken = jwtUtil.generateToken(admin.getId(), admin.getEmail(), admin.getRole().name());
        String refreshToken = refreshTokenService.issue(admin);
        return AuthResponse.of(accessToken, refreshToken,
                admin.getId(), admin.getFullName(), admin.getEmail(), admin.getRole().name());
    }

    private String uniqueSlug(String companyName) {
        String base = companyName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.length() > 40) {
            base = base.substring(0, 40).replaceAll("-+$", "");
        }
        if (base.isEmpty() || base.equals("default")) {
            base = "workspace";
        }
        String slug = base;
        while (organizationRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }
}