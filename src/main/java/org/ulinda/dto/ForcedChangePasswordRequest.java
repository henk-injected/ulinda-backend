package org.ulinda.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForcedChangePasswordRequest {
    @NotBlank
    String username;

    @NotBlank
    @Size(max = 64, message = "Password cannot exceed 64 characters")
    String oldPassword;

    @NotBlank
    @Size(max = 64, message = "Password cannot exceed 64 characters")
    String newPassword;
}
