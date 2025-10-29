package org.ulinda.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ErrorDto {
    private UUID errorIdentifier;
    private Instant timestamp;
    private String message;
}
