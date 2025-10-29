package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
public class UpdateRecordRequest {
    @NotNull
    private UUID recordId;
    @NotNull
    private Map<UUID, Object> fieldValues; // fieldId -> value
}
