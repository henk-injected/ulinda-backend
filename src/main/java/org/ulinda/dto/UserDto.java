package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private String userName;
    private String name;
    private String surname;
    private UUID userId;
    private boolean canCreateModels;
    private boolean adminUser;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;
    private boolean accountDisabled;
}
