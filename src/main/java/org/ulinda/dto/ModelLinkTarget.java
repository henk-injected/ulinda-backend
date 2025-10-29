package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ModelLinkTarget {
    private UUID modelLinkId;
    private UUID targetModelId;
    private boolean can_have_unlimited_targets;
    private Long can_have_targets_count;
    private String targetModelName;
}
