package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.ulinda.enums.ModelPermission;

import java.util.UUID;

@Table("user_model_permissions")
@Data
public class UserModelPermission {

    @Id
    private UUID id;
    private UUID userId;
    private UUID modelId;
    private ModelPermission permission;
}
