package com.doctutorials.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.doctutorials.crm.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ensures auth failures return the same ApiResponse JSON envelope as every
 * other endpoint, instead of an empty body. Spring Security's filter chain
 * intercepts these BEFORE the request reaches any controller, so our normal
 * 
 * @RestControllerAdvice (GlobalExceptionHandler) never sees them - these two
 *                       methods are the filter-chain-level equivalent for
 *                       authentication/authorization
 *                       failures specifically.
 */
@Component
public class SecurityJsonResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** No valid credentials at all (missing/expired/malformed token) -> 401. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Authentication required. Include a valid 'Authorization: Bearer <token>' header.");
    }

    /** Authenticated, but the caller's role doesn't permit this action -> 403. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }
}
