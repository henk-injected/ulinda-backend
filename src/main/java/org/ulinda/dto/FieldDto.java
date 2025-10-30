package org.ulinda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ulinda.enums.FieldType;

import java.util.UUID;

@Data
public class FieldDto {
    private UUID id;
    @NotNull
    private FieldType type;
    @NotNull
    private String name;
    private String description;
    @JsonProperty("isRequired")
    private Boolean isRequired = false;
}
