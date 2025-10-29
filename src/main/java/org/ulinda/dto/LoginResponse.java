package org.ulinda.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private long expiresIn;
    private boolean mustChangePassword;
    private boolean adminUser;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;

    public LoginResponse(String token, String username, long expiresIn) {
        this.token = token;
        this.expiresIn = expiresIn;
    }

    public LoginResponse() {

    }

}
