package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class LinkedRecordDto {
    private UUID recordId;
    private String fields;
    private String targetModelName;
    private UUID targetModelId;
}
