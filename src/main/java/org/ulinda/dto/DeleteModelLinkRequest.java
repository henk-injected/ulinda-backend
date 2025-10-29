package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DeleteModelLinkRequest {
    @NotNull
    private UUID modelLinkId;
}
