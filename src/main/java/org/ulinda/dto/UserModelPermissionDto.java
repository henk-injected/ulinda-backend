package org.ulinda.dto;

import lombok.Data;
import org.ulinda.enums.ModelPermission;

import java.util.List;
import java.util.UUID;

@Data
public class UserModelPermissionDto {
    private UUID modelId;
    private ModelPermission permission;
    private String modelName;
    private String modelDescription;
}
