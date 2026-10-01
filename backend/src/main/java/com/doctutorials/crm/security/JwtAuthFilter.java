package com.doctutorials.crm.security;

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
 * populates the SecurityContext so downstream @PreAuthorize checks work.
 * Any failure here just leaves the request unauthenticated - it never throws,
 * so a bad/missing token degrades to "401 on protected endpoints" rather than
 * a 500 that crashes the request pipeline.
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

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.isTokenValid(token)) {
                    UUID userId = jwtUtil.extractUserId(token);
                    String role = jwtUtil.extractRole(token);

                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent() && userOpt.get().isActive()
                            && SecurityContextHolder.getContext().getAuthentication() == null) {

                        var authToken = new UsernamePasswordAuthenticationToken(
                                userOpt.get(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (Exception ex) {
                // Malformed/expired token: proceed unauthenticated, let Spring Security
                // return 401/403 for protected endpoints instead of blowing up the filter chain.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
