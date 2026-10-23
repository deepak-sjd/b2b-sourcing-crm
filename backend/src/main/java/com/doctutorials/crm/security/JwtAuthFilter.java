package com.doctutorials.crm.security;

import com.doctutorials.crm.tenant.TenantContext;
import com.doctutorials.crm.user.User;
import com.doctutorials.crm.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the "Authorization: Bearer <token>" header, validates it, and if valid,
 * populates the SecurityContext AND the TenantContext (taken from the user's
 * organization in the database, never from a client-supplied value).
 * Any failure just leaves the request unauthenticated and tenant-less.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return;
        }
        String token = header.substring(7);
        try {
            if (!jwtUtil.isTokenValid(token)) {
                return;
            }
            UUID userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);

            Optional<User> userOpt = userRepository.findUnscopedById(userId);
            if (userOpt.isPresent() && userOpt.get().isActive()
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = userOpt.get();
                var authToken = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                TenantContext.set(user.getOrganization().getId());
            }
        } catch (Exception ex) {
            // Malformed/expired token: proceed unauthenticated and tenant-less.
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }
}