package org.ulinda.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.ulinda.dto.GenerateTokenRequest;
import org.ulinda.dto.GenerateTokenResponse;
import org.ulinda.dto.GetUserTokensResponse;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.services.TokenService;

import java.util.UUID;

@RestController
@RequestMapping("/api/tokens")
@Slf4j
public class TokenController {

    @Autowired
    private TokenService tokenService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateTokenResponse> generateToken(
            @Valid @RequestBody GenerateTokenRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new FrontendException("User not authenticated", ErrorCode.UNAUTHORIZED, true);
        }

        UUID userId = (UUID) authentication.getPrincipal();
        GenerateTokenResponse response = tokenService.generateUserToken(userId, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-tokens")
    public ResponseEntity<GetUserTokensResponse> getMyTokens(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new FrontendException("User not authenticated", ErrorCode.UNAUTHORIZED, true);
        }

        UUID userId = (UUID) authentication.getPrincipal();
        GetUserTokensResponse response = tokenService.getUserTokens(userId);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<Void> deleteToken(
            @PathVariable UUID tokenId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new FrontendException("User not authenticated", ErrorCode.UNAUTHORIZED, true);
        }

        UUID userId = (UUID) authentication.getPrincipal();
        tokenService.deleteUserToken(userId, tokenId);

        return ResponseEntity.noContent().build();
    }
}
