package org.ulinda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateFieldRequest {
    @NotBlank
    private String name;
    private String description;
    @JsonProperty("isParent")
    private boolean isParent = false;
    @JsonProperty("isRequired")
    private boolean isRequired = false;
}
