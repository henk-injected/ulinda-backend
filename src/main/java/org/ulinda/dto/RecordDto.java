package org.ulinda.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class RecordDto {
    private UUID id;
    private java.time.Instant createdAt;
    private java.time.Instant updatedAt;
    private Map<UUID, Object> fieldValues;
    private List<LinkedRecordCount> linkedRecordCounts;
    private UUID linkId;
}