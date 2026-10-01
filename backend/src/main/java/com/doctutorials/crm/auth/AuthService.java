package com.doctutorials.crm.auth;

import com.doctutorials.crm.auth.dto.AuthResponse;
import com.doctutorials.crm.auth.dto.LoginRequest;
import com.doctutorials.crm.auth.token.PasswordResetToken;
import com.doctutorials.crm.auth.token.PasswordResetTokenRepository;
import com.doctutorials.crm.auth.token.RefreshTokenService;
import com.doctutorials.crm.auth.token.TokenHasher;
import com.doctutorials.crm.common.exception.AccountLockedException;
import com.doctutorials.crm.common.exception.BadRequestException;
import com.doctutorials.crm.mail.MailService;
import com.doctutorials.crm.security.JwtUtil;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import com.doctutorials.crm.user.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHasher tokenHasher;
    private final MailService mailService;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrlBase;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.isLocked()) {
            throw new AccountLockedException(user.getLockedUntil());
        }

        try {
            // Delegates password checking to Spring Security; throws
            // BadCredentialsException
            // on mismatch (or DisabledException if the account was deactivated).
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            userService.recordFailedLoginAttempt(user);
            throw ex;
        }

        userService.resetFailedLoginAttempts(user);

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);

        return AuthResponse.of(accessToken, refreshToken,
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedTokens rotated = refreshTokenService.rotate(rawRefreshToken);
        User user = rotated.user();

        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.of(accessToken, rotated.rawRefreshToken(),
                user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /**
     * Always returns normally, whether or not the email matches an account.
     * Callers (AuthController) return the same generic response either way -
     * this is what prevents the endpoint being used to enumerate registered
     * email addresses.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = tokenHasher.generateRawToken();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(tokenHasher.hash(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = resetPasswordUrlBase + "?token=" + rawToken;
            mailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset link"));

        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Invalid or expired reset link");
        }

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        User user = token.getUser();
        userService.setPassword(user, newPassword);

        // A password reset is a strong signal the old credential may have been
        // compromised - kill every existing session so a stolen access/refresh
        // token pair stops working the moment the password changes.
        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password reset completed for user {}", user.getId());
    }
}
