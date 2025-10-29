package org.ulinda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Size(max = 64, message = "Password cannot exceed 64 characters")
    private String password;
}
