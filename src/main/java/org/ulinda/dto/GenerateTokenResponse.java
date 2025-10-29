package org.ulinda.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class GenerateTokenResponse {
    private String token;
    private String tokenName;
    private Instant expiryDateTime;
}
