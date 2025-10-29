package org.ulinda.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetUserModelPermissionsResponse {
    private List<UserModelPermissionDto> userModelPermissions;
}
