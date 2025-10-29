package org.ulinda.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AdminTokenDto {
    private UUID id;
    private UUID userId;
    private String username;
    private String tokenName;
    private String tokenPrefix;
    private Instant createdAt;
    private Instant tokenExpiryDateTime;
}
