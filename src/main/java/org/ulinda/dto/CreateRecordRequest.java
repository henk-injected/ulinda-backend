package org.ulinda.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class CreateRecordRequest {
    private UUID modelId;
    private Map<UUID, Object> fieldValues; // fieldId -> value
}
