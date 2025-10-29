package org.ulinda.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.ulinda.dto.*;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.services.SecuritySettingsService;
import org.ulinda.services.SessionService;
import org.ulinda.services.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/private-auth")
@Slf4j
public class PrivateAuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/change-password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new FrontendException("User not authenticated", ErrorCode.UNAUTHORIZED, true);
        }
        UUID userId = (UUID) authentication.getPrincipal();
        if (userService.validatePassword(userId, changePasswordRequest.getOldPassword())) {
            userService.changePassword(userId, changePasswordRequest.getNewPassword());
        } else {
            throw new FrontendException("Incorrect old password", ErrorCode.OLD_PASSWORD_INCORRECT, true);
        }
    }
}
