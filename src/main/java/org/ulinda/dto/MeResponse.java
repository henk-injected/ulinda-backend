package org.ulinda.dto;

import lombok.Data;

@Data
public class MeResponse {
    private String username;
    private boolean adminUser;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;
}
