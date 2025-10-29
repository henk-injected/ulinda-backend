package org.ulinda.repositories;

import org.springframework.data.repository.CrudRepository;
import org.ulinda.entities.UserModelPermission;

import java.util.List;
import java.util.UUID;

public interface UserModelPermissionRepository extends CrudRepository<UserModelPermission, UUID> {
    List<UserModelPermission> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
