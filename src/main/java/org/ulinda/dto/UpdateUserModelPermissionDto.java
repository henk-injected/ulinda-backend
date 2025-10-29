package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ulinda.enums.ModelPermission;

import java.util.UUID;

@Data
public class UpdateUserModelPermissionDto {
    @NotNull
    private UUID modelId;
    @NotNull
    private ModelPermission permission;
}
