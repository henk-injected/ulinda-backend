package org.ulinda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.ulinda.entities.CurrentUserToken;
import org.ulinda.repositories.CurrentUserTokenRepository;
import org.ulinda.services.SessionService;
import org.ulinda.services.UserService;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class AuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;
    private final SessionService sessionService;
    private final CurrentUserTokenRepository currentUserTokenRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int TOKEN_PREFIX_LENGTH = 14; // "uli_" + 10 chars

    public AuthenticationFilter(UserService userService, SessionService sessionService, CurrentUserTokenRepository currentUserTokenRepository, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.currentUserTokenRepository = currentUserTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            throw new RuntimeException("Authentication already exists");
        }

        // Skip authentication for public auth endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("Processing authentication for path: {}", path);

        UUID userId = null;
        boolean isAuthenticated = false;

        // Try to authenticate via Bearer token (API token)
        final String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Check if it's an API token (starts with uli_)
            if (token.startsWith("uli_")) {
                try {
                    // Extract token prefix for lookup (first 14 characters)
                    String tokenPrefix = token.substring(0, Math.min(TOKEN_PREFIX_LENGTH, token.length()));

                    // Look up tokens by prefix (faster than checking all tokens)
                    List<CurrentUserToken> candidateTokens = currentUserTokenRepository.findAllByTokenPrefix(tokenPrefix);

                    // Check each candidate token by comparing hashes
                    for (CurrentUserToken candidate : candidateTokens) {
                        // Verify hash matches
                        if (passwordEncoder.matches(token, candidate.getTokenHash())) {
                            // Check if token is expired
                            if (candidate.getTokenExpiryDateTime().isAfter(Instant.now())) {
                                userId = candidate.getUserId();
                                isAuthenticated = true;
                                log.debug("API token validated for user: {} (prefix: {})", userId, tokenPrefix);
                                break; // Token found and valid
                            } else {
                                log.warn("API token expired for user: {} (prefix: {})", candidate.getUserId(), tokenPrefix);
                                break; // Token found but expired
                            }
                        }
                    }

                    if (!isAuthenticated && !candidateTokens.isEmpty()) {
                        log.warn("API token hash mismatch for prefix: {}", tokenPrefix);
                    } else if (candidateTokens.isEmpty()) {
                        log.warn("No API token found with prefix: {}", tokenPrefix);
                    }
                } catch (Exception e) {
                    log.error("API token validation failed", e);
                }
            } else {
                log.warn("Invalid bearer token format. Only uli_ tokens are supported");
            }
        }

        // If not authenticated via API token, try session cookie
        if (!isAuthenticated) {
            UUID sessionId = null;
            Cookie[] cookies = request.getCookies();

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("SESSION_ID".equals(cookie.getName())) {
                        try {
                            sessionId = UUID.fromString(cookie.getValue());
                            break;
                        } catch (Exception e) {
                            log.error("Invalid session ID format in cookie", e);
                        }
                    }
                }
            }

            if (sessionId != null) {
                try {
                    userId = sessionService.validateSessionId(sessionId);
                    isAuthenticated = true;
                    log.debug("Session cookie validated for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Session validation failed for sessionId: {}", sessionId, e);
                }
            }
        }

        // Set authentication in SecurityContext if authenticated
        if (userId != null && isAuthenticated) {
            try {
                // Load user details and check admin status
                org.ulinda.dto.GetUserResponse user = userService.getUser(userId);

                if (user != null && !user.isAccountDisabled()) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (user.isAdminUser()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("Authentication successful for user: {}", userId);
                } else {
                    log.warn("Authentication failed for user: {}", userId);
                    if (user == null) {
                        log.warn("User is null : " + userId);
                    }
                    if (user != null && user.isAccountDisabled()) {
                        log.warn("Account disabled for user: {}", userId);
                    }
                }

            } catch (Exception e) {
                log.error("Error loading user details for userId: {}", userId, e);
                // SecurityContext remains empty - Spring Security will handle as unauthenticated
            }
        }

        // Always continue filter chain - Spring Security will handle 401 for unauthenticated requests
        filterChain.doFilter(request, response);
    }
}