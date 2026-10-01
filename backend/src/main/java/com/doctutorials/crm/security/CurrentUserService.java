package com.doctutorials.crm.security;

import com.doctutorials.crm.common.exception.UnauthorizedException;
import com.doctutorials.crm.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Single place that resolves "who is making this request" as a full User
 * entity. JwtAuthFilter already sets the User itself (not just an ID/email)
 * as the Authentication principal, so this is just a safe, reusable cast -
 * every row-level-security check in the app should go through this rather
 * than reading SecurityContextHolder directly, so there's one place to
 * change if that ever stops being true (e.g. a future switch to a lighter
 * principal object).
 */
@Service
public class CurrentUserService {

    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            // Should be unreachable on any endpoint behind SecurityConfig's
            // "anyRequest().authenticated()" - if this fires, something is
            // misconfigured, not just "user not logged in".
            throw new UnauthorizedException("No authenticated user in request context");
        }
        return user;
    }
}
