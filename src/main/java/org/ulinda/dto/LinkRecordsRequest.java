package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LinkRecordsRequest {
    @NotNull
    private UUID modelLinkId;
    @NotNull
    private UUID sourceModelId;
    @NotNull
    private UUID sourceRecordId;
    @NotNull
    private UUID targetRecordId;
}
