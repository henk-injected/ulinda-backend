package org.ulinda.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UpdateModelRequest {
    @NotEmpty
    private String modelName;
    private String modelDescription;
}
