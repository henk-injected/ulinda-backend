package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.dto.*;
import org.ulinda.entities.CurrentUserToken;
import org.ulinda.entities.User;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.repositories.CurrentUserTokenRepository;
import org.ulinda.repositories.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TokenService {

    @Autowired
    private CurrentUserTokenRepository currentUserTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TOKEN_PREFIX = "uli_";
    private static final int TOKEN_LENGTH = 40; // 40 random characters
    private static final int TOKEN_PREFIX_LENGTH = 14; // "uli_" + 10 chars for display

    @Transactional
    public GenerateTokenResponse generateUserToken(UUID userId, GenerateTokenRequest request) {
        // Get user and validate
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user can generate tokens
        if (!user.isCanGenerateTokens()) {
            throw new FrontendException("User is not allowed to generate tokens", ErrorCode.PERMISSION_DENIED, true);
        }

        // Get existing tokens
        List<CurrentUserToken> existingTokens = currentUserTokenRepository.findAllByUserId(userId);

        // Check if user has reached max token count
        if (existingTokens.size() >= user.getMaxTokenCount()) {
            throw new FrontendException(
                    "Maximum token count reached (" + user.getMaxTokenCount() + "). Please delete an existing token first.",
                    ErrorCode.GENERAL_ERROR,
                    true);
        }

        // Generate random token (plain text - will be shown to user once)
        String token = generateRandomToken();

        // Hash the token for secure storage
        String tokenHash = passwordEncoder.encode(token);

        // Extract prefix for lookup and display (first 14 chars: "uli_" + 10 more)
        String tokenPrefix = token.substring(0, Math.min(TOKEN_PREFIX_LENGTH, token.length()));

        // Calculate expiry date
        Instant expiryDateTime = Instant.now().plus(request.getExpiryDays(), ChronoUnit.DAYS);

        // Save hashed token to database
        CurrentUserToken currentUserToken = new CurrentUserToken();
        currentUserToken.setUserId(userId);
        currentUserToken.setTokenHash(tokenHash);
        currentUserToken.setTokenPrefix(tokenPrefix);
        currentUserToken.setTokenName(request.getTokenName());
        currentUserToken.setCreatedAt(Instant.now());
        currentUserToken.setTokenExpiryDateTime(expiryDateTime);
        currentUserTokenRepository.save(currentUserToken);

        // Prepare response with FULL token (shown only once!)
        GenerateTokenResponse response = new GenerateTokenResponse();
        response.setToken(token);  // Full unhashed token
        response.setTokenName(request.getTokenName());
        response.setExpiryDateTime(expiryDateTime);

        log.info("Generated new token for user: {} with name: {} (prefix: {})", userId, request.getTokenName(), tokenPrefix);

        return response;
    }

    @Transactional(readOnly = true)
    public GetUserTokensResponse getUserTokens(UUID userId) {
        List<CurrentUserToken> tokens = currentUserTokenRepository.findAllByUserId(userId);

        List<UserTokenDto> tokenDtos = tokens.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        GetUserTokensResponse response = new GetUserTokensResponse();
        response.setTokens(tokenDtos);

        return response;
    }

    @Transactional
    public void deleteUserToken(UUID userId, UUID tokenId) {
        CurrentUserToken token = currentUserTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // Verify the token belongs to the user
        if (!token.getUserId().equals(userId)) {
            throw new FrontendException("You can only delete your own tokens", ErrorCode.PERMISSION_DENIED, true);
        }

        currentUserTokenRepository.delete(token);
        log.info("Deleted token {} for user: {}", tokenId, userId);
    }

    @Transactional(readOnly = true)
    public GetAllTokensResponse getAllTokens(Pageable pageable) {
        Page<CurrentUserToken> tokensPage = currentUserTokenRepository.findAll(pageable);

        List<AdminTokenDto> tokenDtos = tokensPage.getContent().stream()
                .map(this::convertToAdminDto)
                .collect(Collectors.toList());

        GetAllTokensResponse response = new GetAllTokensResponse();
        response.setTokens(tokenDtos);
        response.setPagingInfo(new TokenPagingInfo(tokensPage));

        return response;
    }

    @Transactional
    public void deleteTokenAdmin(UUID tokenId) {
        CurrentUserToken token = currentUserTokenRepository.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        currentUserTokenRepository.delete(token);
        log.info("Admin deleted token {} for user: {}", tokenId, token.getUserId());
    }

    private UserTokenDto convertToDto(CurrentUserToken token) {
        UserTokenDto dto = new UserTokenDto();
        dto.setId(token.getId());
        dto.setTokenName(token.getTokenName());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setTokenExpiryDateTime(token.getTokenExpiryDateTime());

        // Show the stored prefix (first 14 characters) with ellipsis
        dto.setTokenPrefix(token.getTokenPrefix() + "...");

        return dto;
    }

    private AdminTokenDto convertToAdminDto(CurrentUserToken token) {
        AdminTokenDto dto = new AdminTokenDto();
        dto.setId(token.getId());
        dto.setUserId(token.getUserId());
        dto.setTokenName(token.getTokenName());
        dto.setCreatedAt(token.getCreatedAt());
        dto.setTokenExpiryDateTime(token.getTokenExpiryDateTime());
        dto.setTokenPrefix(token.getTokenPrefix() + "...");

        // Get username from userRepository
        User user = userRepository.findById(token.getUserId()).orElse(null);
        dto.setUsername(user != null ? user.getUsername() : "Unknown");

        return dto;
    }

    private String generateRandomToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Ensure it's exactly TOKEN_LENGTH characters
        if (randomPart.length() > TOKEN_LENGTH) {
            randomPart = randomPart.substring(0, TOKEN_LENGTH);
        }

        return TOKEN_PREFIX + randomPart;
    }
}
