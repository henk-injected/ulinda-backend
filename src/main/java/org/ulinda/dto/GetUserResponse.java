package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class GetUserResponse {
    private UUID userId;
    private String userName;
    private String name;
    private String surname;
    private boolean adminUser;
    private boolean canCreateModels;
    private boolean mustChangePassword;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;
    private boolean accountDisabled;
}
