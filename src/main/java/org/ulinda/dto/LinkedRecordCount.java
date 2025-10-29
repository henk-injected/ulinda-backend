package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class LinkedRecordCount {
    private UUID linkId;
    private String targetModelName;
    private UUID targetModelId;
    private Long recordCount;
}
