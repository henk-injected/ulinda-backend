package org.ulinda.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateTokenRequest {
    @NotBlank(message = "Token name cannot be blank")
    private String tokenName;

    @NotNull(message = "Expiry days cannot be null")
    @Min(value = 1, message = "Expiry days must be at least 1")
    private Integer expiryDays;
}
