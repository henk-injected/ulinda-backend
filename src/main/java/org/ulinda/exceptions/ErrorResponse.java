package org.ulinda.exceptions;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ErrorResponse {
    private ErrorCode errorCode;
    private String message;
    private Instant timestamp = Instant.now();
    private UUID errorIdentifier;
    private boolean showMessageToUser = false;
}
