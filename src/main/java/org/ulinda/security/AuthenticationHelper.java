package org.ulinda.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthenticationHelper {

//    @Autowired
//    UserRoleRepository userRoleRepository;
//
//    public List<UserRole> getUserRoles(Authentication authentication) {
//        UUID userId = (UUID) authentication.getPrincipal();
//        List<UserRoleEntity> userRoleList = userRoleRepository.findByUserId(userId);
//        List<UserRole> userRoles = new ArrayList<>();
//        for (UserRoleEntity userRole : userRoleList) {
//            userRoles.add(userRole.getRole());
//        }
//        return userRoles;
//    }

    public UUID getUserId(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        if (userId == null) {
            throw new RuntimeException("No user id found");
        }
        return userId;
    }
}
