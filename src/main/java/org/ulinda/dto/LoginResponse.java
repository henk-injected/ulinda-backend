package org.ulinda.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private boolean mustChangePassword;
    private boolean adminUser;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;

    public LoginResponse() {

    }

}
