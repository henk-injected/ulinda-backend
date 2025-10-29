package org.ulinda.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserTokenDto {
    private UUID id;
    private String tokenName;
    private Instant createdAt;
    private Instant tokenExpiryDateTime;
    private String tokenPrefix; // Only show first few characters for security
}
