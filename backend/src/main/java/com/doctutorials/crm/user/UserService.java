package com.doctutorials.crm.user;

import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.common.exception.ResourceNotFoundException;
import com.doctutorials.crm.organization.Organization;
import com.doctutorials.crm.organization.OrganizationRepository;
import com.doctutorials.crm.security.CurrentUserService;
import com.doctutorials.crm.team.Team;
import com.doctutorials.crm.team.TeamRepository;
import com.doctutorials.crm.user.dto.PublicRegisterRequest;
import com.doctutorials.crm.user.dto.RegisterRequest;
import com.doctutorials.crm.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.max-failed-login-attempts}")
    private int maxFailedLoginAttempts;

    @Value("${app.security.lockout-duration-minutes}")
    private long lockoutDurationMinutes;

    /**
     * PUBLIC self-registration. No role or team can be supplied by the
     * caller — every self-registered account is created as COUNSELOR with
     * no team. This is the only registration path reachable without
     * authentication, so it must never accept a role from the request.
     *
     * Organization: there is no authenticated caller here to derive one from
     * (that's the whole point of "public"), and how a public sign-up should
     * pick its organization - subdomain, invite link, a company field on the
     * form - is exactly the open multi-tenancy decision still pending with
     * the client. Every self-registration lands in the single "default"
     * organization for now, which is behaviorally identical to today (there
     * is currently only one organization), and is a deliberate placeholder,
     * not a real answer to that question.
     */
    @Transactional
    public User registerPublic(PublicRegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BadRequestException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(req.fullName());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.COUNSELOR);
        user.setOrganization(defaultOrganization());

        return userRepository.save(user);
    }

    private Organization defaultOrganization() {
        return organizationRepository.findBySlug("default")
                .orElseThrow(() -> new IllegalStateException(
                        "No 'default' organization found - V6__organizations.sql should have created one"));
    }

    /**
     * ADMIN-only user creation. Only reachable via UserController, which is
     * class-level @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')") — this
     * is the sole path through which a role or team can be assigned.
     */
    @Transactional
    public User createByAdmin(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw new BadRequestException("A user with this email already exists");
        }

        User user = new User();
        user.setFullName(req.fullName());
        user.setEmail(req.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role() != null ? req.role() : Role.COUNSELOR);
        // New user belongs to whichever organization the admin creating them belongs to -
        // an admin can't create a user in someone else's organization.
        user.setOrganization(currentUserService.get().getOrganization());

        if (req.teamId() != null) {
            Team team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Team", req.teamId()));
            user.setTeam(team);
        }

        return userRepository.save(user);
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ResourceNotFoundException.of("User", email));
    }

    public List<UserResponse> listAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = getById(id);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Called after a failed login. Increments the failure counter and, once
     * it hits the configured threshold, locks the account for
     * lockoutDurationMinutes and resets the counter so the next window
     * starts clean once the lock expires.
     */
    @Transactional
    public void recordFailedLoginAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= maxFailedLoginAttempts) {
            user.setLockedUntil(Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    /**
     * Used by password reset. Deliberately does not check the old password -
     * that's the whole point of a reset flow - so this must only ever be
     * called after PasswordResetToken has already been validated.
     */
    @Transactional
    public void setPassword(User user, String newRawPassword) {
        user.setPasswordHash(passwordEncoder.encode(newRawPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}
