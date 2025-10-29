package org.ulinda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String name;
    private String surname;
    @Pattern(regexp = "^[a-zA-Z0-9]+$",
            message = "Username must contain only letters and numbers, no spaces allowed")
    @NotBlank(message = "Username cannot be blank")
    private String username;
    private boolean canCreateModels = false;
    private boolean adminUser;
    private boolean canGenerateTokens = false;
    @NotNull(message = "Max token count cannot be null")
    private Integer maxTokenCount = 5;
}
