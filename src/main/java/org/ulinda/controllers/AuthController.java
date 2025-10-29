package org.ulinda.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.ulinda.dto.*;
import org.ulinda.entities.User;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.services.SecuritySettingsService;
import org.ulinda.services.SessionService;
import org.ulinda.services.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserService userService;
    private final SessionService sessionService;
    private final long jwtExpiration;
    private final SecuritySettingsService securitySettingsService;

    public AuthController(UserService userService,
                          SessionService sessionService,
                          @Value("${ULINDA_JWT_EXPIRATION}") long jwtExpiration,
                          SecuritySettingsService securitySettingsService) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.jwtExpiration = jwtExpiration;
        this.securitySettingsService = securitySettingsService;
    }


    /**
     * EXCLUDED FROM JWT CHECK
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        String username = loginRequest.getUsername();

        // Check if account is locked
        if (userService.isAccountLocked(username)) {
            throw new FrontendException("Account is temporarily locked due to too many failed login attempts. Please try again later.", ErrorCode.ACCOUNT_LOCKED, true);
        }

        if (userService.validateUser(username, loginRequest.getPassword())) {
            // Reset failed login attempts on successful login
            userService.resetFailedLoginAttempts(username);

            UUID userId = userService.getUserId(username);
            //Check if user must change password
            GetUserResponse user = userService.getUser(userId);
            if (user.isMustChangePassword()) {
                throw new FrontendException("User must change password", ErrorCode.USER_MUST_CHANGE_PASSWORD, true);
            }

            // Get client IP address
            String ipAddress = getClientIpAddress(request);

            // Create session in database
            UUID sessionId = sessionService.createSession(userId, ipAddress);

            // Create secure cookie
            Cookie sessionCookie = new Cookie("SESSION_ID", sessionId.toString());
            sessionCookie.setHttpOnly(true);
            sessionCookie.setSecure(true);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(Math.toIntExact(jwtExpiration / 1000)); // Convert ms to seconds
            response.addCookie(sessionCookie);

            // Return response without token
            LoginResponse loginResponse = new LoginResponse(null, userId.toString(), jwtExpiration);
            loginResponse.setAdminUser(user.isAdminUser());
            loginResponse.setCanGenerateTokens(user.isCanGenerateTokens());
            loginResponse.setMaxTokenCount(user.getMaxTokenCount());
            return ResponseEntity.ok(loginResponse);
        } else {
            // Increment failed login attempts
            userService.incrementFailedLoginAttempts(username);
            throw new FrontendException("Invalid Credentials", ErrorCode.INVALID_LOGIN_CREDENTIALS, true);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/forced-change-password")
    public void forcedChangePassword(@Valid @RequestBody ForcedChangePasswordRequest request) {
        userService.forcedChangePassword(request.getUsername(), request.getOldPassword(), request.getNewPassword());
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> getMeInfo(HttpServletRequest request) {
        UUID sessionId = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION_ID".equals(cookie.getName())) { // Use your cookie name
                    try {
                        sessionId = UUID.fromString(cookie.getValue());
                    }  catch (Exception e) {
                        log.error("Invalid session ID", e);
                        throw new FrontendException("Session Expired", ErrorCode.SESSION_EXPIRED, true);
                    }
                    break;
                }
            }
        }
        if (sessionId != null) {
            try {
                UUID userId = sessionService.validateSessionId(sessionId);
                GetUserResponse user = userService.getUser(userId);
                MeResponse response = new MeResponse();
                response.setUsername(user.getUserName());
                response.setAdminUser(user.isAdminUser());
                response.setCanGenerateTokens(user.isCanGenerateTokens());
                response.setMaxTokenCount(user.getMaxTokenCount());
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                log.error("Invalid session ID", e);
                throw new FrontendException("Session Expired", ErrorCode.SESSION_EXPIRED, true);
            }
        }
        throw new FrontendException("Session Expired", ErrorCode.SESSION_EXPIRED, true);
    }

    @GetMapping("/password-settings")
    public ResponseEntity<PasswordSettings> getPasswordSettings() {
        return ResponseEntity.ok(securitySettingsService.getPasswordSettings());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        UUID sessionId = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION_ID".equals(cookie.getName())) {
                    try {
                        sessionId = UUID.fromString(cookie.getValue());
                    } catch (Exception e) {
                        log.error("Invalid session ID", e);
                    }
                    break;
                }
            }
        }

        // Always try to invalidate session if found
        if (sessionId != null) {
            try {
                sessionService.invalidateSession(sessionId);
            } catch (Exception e) {
                log.error("Could not invalidate session (logout)", e);
                // Don't throw - logout should be idempotent
            }
        }

        // IMPORTANT: Clear the cookie on the client side
        Cookie clearCookie = new Cookie("SESSION_ID", "");
        clearCookie.setMaxAge(0);  // Expire immediately
        clearCookie.setPath("/");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);  // Match your original cookie settings
        response.addCookie(clearCookie);
    }
}
