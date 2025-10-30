package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.ulinda.dto.*;
import org.ulinda.entities.CurrentUserToken;
import org.ulinda.entities.Model;
import org.ulinda.entities.User;
import org.ulinda.entities.UserModelPermission;
import org.ulinda.exceptions.ErrorCode;
import org.ulinda.exceptions.FrontendException;
import org.ulinda.repositories.CurrentUserTokenRepository;
import org.ulinda.repositories.ModelRepository;
import org.ulinda.repositories.UserModelPermissionRepository;
import org.ulinda.repositories.UserRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserService {

    @Value("${ULINDA_ADMIN_PASSWORD:}")
    private String adminUserPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordService passwordService;
    private final UserModelPermissionRepository userModelPermissionRepository;
    private final ModelRepository modelRepository;
    private final CurrentUserTokenRepository currentUserTokenRepository;
    private final PasswordValidationService passwordValidationService;
    private final SessionService sessionService;
    private final SecuritySettingsService securitySettingsService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordService passwordService,
            UserModelPermissionRepository userModelPermissionRepository,
            ModelRepository modelRepository,
            CurrentUserTokenRepository currentUserTokenRepository,
            PasswordValidationService passwordValidationService,
            SessionService sessionService,
            SecuritySettingsService securitySettingsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordService = passwordService;
        this.userModelPermissionRepository = userModelPermissionRepository;
        this.modelRepository = modelRepository;
        this.currentUserTokenRepository = currentUserTokenRepository;
        this.passwordValidationService = passwordValidationService;
        this.sessionService = sessionService;
        this.securitySettingsService = securitySettingsService;
    }

    @Transactional
    public UUID createAdminUser() {
        if (StringUtils.isEmpty(adminUserPassword)) {
            throw new RuntimeException("ULINDA_ADMIN_PASSWORD environment variable not set");
        }
        if (!userRepository.existsByUsername("admin")) {
            String encryptedPassword = passwordEncoder.encode(adminUserPassword);
            User admin = new User("admin", encryptedPassword, "Admin", "User", true, true, true, 10);
            userRepository.save(admin);
            UUID userId = admin.getId();
            if (userId == null) {
                throw new RuntimeException("User ID is null");
            }
            return userId;
        }
        throw new RuntimeException("Admin user already exists");
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest createUserRequest) {
        String username = createUserRequest.getUsername().toLowerCase();
        String password = passwordService.generatePassword();
        if (!userRepository.existsByUsername(username)) {
            String encryptedPassword = passwordEncoder.encode(password);
            User user = new User(username, encryptedPassword, createUserRequest.getName(), createUserRequest.getSurname(),
                    createUserRequest.isCanCreateModels(), createUserRequest.isAdminUser(),
                    createUserRequest.isCanGenerateTokens(), createUserRequest.getMaxTokenCount());
            user.setMustChangePassword(true);
            userRepository.save(user);
            CreateUserResponse response = new CreateUserResponse();
            response.setUsername(username);
            response.setPassword(password);
            return response;
        }
        throw new FrontendException("Username already exists", ErrorCode.USER_ALREADY_EXISTS, true);
    }

    @Transactional(readOnly = true)
    public boolean validateUser(String username, String password) {
        username = username.toLowerCase();
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent() && passwordEncoder.matches(password, user.get().getPassword());
    }

    @Transactional(readOnly = true)
    public boolean validatePassword(UUID userId, String password) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return passwordEncoder.matches(password, user.getPassword());
    }

    @Transactional
    public void changePassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Validate new password against security settings
        PasswordValidationService.PasswordValidationResult validationResult =
                passwordValidationService.validatePasswordWithUsername(newPassword, user.getUsername());

        if (!validationResult.isValid()) {
            throw new FrontendException(validationResult.getErrorMessage(), ErrorCode.PASSWORD_REQUIREMENT_FAILED, true);
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        // Kill all active sessions for this user
        sessionService.deleteSessionsForUser(userId);
    }

    @Transactional
    public void forcedChangePassword(String username, String oldPassword , String newPassword) {

        if  (!StringUtils.hasText(username) || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new RuntimeException("Invalid parameters");
        }
        if (!userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username not found");
        }
        //Check to see if user was forced
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isMustChangePassword() != true) {
            throw new RuntimeException("User was not forced to change password");
        }

        //Check old password provided
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new FrontendException("Old password doesn't match", ErrorCode.OLD_PASSWORD_INCORRECT, true);
        }

        // Validate new password against security settings
        PasswordValidationService.PasswordValidationResult validationResult =
                passwordValidationService.validatePasswordWithUsername(newPassword, user.getUsername());

        if (!validationResult.isValid()) {
            throw new FrontendException(validationResult.getErrorMessage(), ErrorCode.PASSWORD_REQUIREMENT_FAILED, true);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Kill all active sessions for this user
        sessionService.deleteSessionsForUser(user.getId());

    }

    @Transactional
    public String resetPassword(final UUID uuid) {
        //Validate user id
        User user = userRepository.findById(uuid).orElseThrow(() -> new RuntimeException("User not found"));
        final String newPassword = passwordService.generatePassword();

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(true);
        userRepository.save(user);

        // Kill all active sessions for this user
        sessionService.deleteSessionsForUser(uuid);

        return newPassword;

    }

    @Transactional(readOnly = true)
    public UUID getUserId(String username) {
        username = username.toLowerCase();
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isPresent()) {
            return user.get().getId();
        } else {
            throw new RuntimeException("Username not found");
        }
    }

    @Transactional(readOnly = true)
    public List<UserDto> getUsers() {
        List<UserDto> users = new ArrayList<>();
        userRepository.findAll().forEach(user -> {
            UserDto userDto = new UserDto();
            userDto.setUserName(user.getUsername());
            userDto.setName(user.getName());
            userDto.setSurname(user.getSurname());
            userDto.setUserId(user.getId());
            userDto.setCanCreateModels(user.isCanCreateModels());
            userDto.setAdminUser(user.isAdminUser());
            userDto.setCanGenerateTokens(user.isCanGenerateTokens());
            userDto.setMaxTokenCount(user.getMaxTokenCount());
            userDto.setAccountDisabled(user.isAccountDisabled());
            users.add(userDto);
        });
        return users;
    }

    @Transactional(readOnly = true)
    public GetUserResponse getUser(UUID userId) {
        GetUserResponse response = new GetUserResponse();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));
        response.setUserId(userId);
        response.setUserName(user.getUsername());
        response.setName(user.getName());
        response.setSurname(user.getSurname());
        response.setCanCreateModels(user.isCanCreateModels());
        response.setAdminUser(user.isAdminUser());
        response.setMustChangePassword(user.isMustChangePassword());
        response.setCanGenerateTokens(user.isCanGenerateTokens());
        response.setMaxTokenCount(user.getMaxTokenCount());
        response.setAccountDisabled(user.isAccountDisabled());
        return response;
    }

    @Transactional(readOnly = true)
    public GetUserModelPermissionsResponse getUserModelPermissions(UUID userId) {
        GetUserModelPermissionsResponse response = new GetUserModelPermissionsResponse();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found: " + userId));
        List<UserModelPermission> permissions = userModelPermissionRepository.findByUserId(user.getId());
        List<UserModelPermissionDto> userModelPermissions = new ArrayList<>();
        for (UserModelPermission permission : permissions) {
            Model model = modelRepository.findById(permission.getModelId()).orElseThrow(() -> new RuntimeException("Model not found: " + permission.getModelId()));
            UserModelPermissionDto userModelPermissionDto = new UserModelPermissionDto();
            userModelPermissionDto.setModelName(model.getName());
            userModelPermissionDto.setModelId(model.getId());
            userModelPermissionDto.setModelDescription(model.getDescription());
            userModelPermissionDto.setPermission(permission.getPermission());
            userModelPermissions.add(userModelPermissionDto);
        }
        response.setUserModelPermissions(userModelPermissions);
        return response;
    }

    @Transactional
    public void updateUser(UpdateUserRequest updateUserRequest) {
        UUID userId = updateUserRequest.getUserId();
        User user = userRepository.findById(updateUserRequest.getUserId()).orElseThrow(() -> new RuntimeException("User not found: " + updateUserRequest.getUserId()));

        if (user.getUsername().toLowerCase().equals("admin")) {
            log.info("Administrator (admin) user's settings can't be changed");
            return;
        }
        user.setUsername(updateUserRequest.getUsername().toLowerCase());
        user.setName(updateUserRequest.getName());
        user.setSurname(updateUserRequest.getSurname());
        user.setCanCreateModels(updateUserRequest.isCanCreateModels());
        user.setAdminUser(updateUserRequest.isAdminUser());
        user.setMustChangePassword(updateUserRequest.isMustChangePassword());
        user.setCanGenerateTokens(updateUserRequest.isCanGenerateTokens());
        user.setMaxTokenCount(updateUserRequest.getMaxTokenCount());
        user.setAccountDisabled(updateUserRequest.isAccountDisabled());
        userRepository.save(user);
        userModelPermissionRepository.deleteByUserId(userId);

        for (UpdateUserModelPermissionDto permission : updateUserRequest.getPermissions()) {
            UserModelPermission userModelPermission = new UserModelPermission();
            userModelPermission.setUserId(userId);
            userModelPermission.setModelId(permission.getModelId());
            userModelPermission.setPermission(permission.getPermission());
            userModelPermissionRepository.save(userModelPermission);
        }

        //Check if user must change the password
        if (updateUserRequest.isMustChangePassword()) {
            //Delete all sessions for user
            sessionService.deleteSessionsForUser(userId);
        }
    }

    @Transactional(readOnly = true)
    public UUID getAdminUserId() {
        User user = userRepository.findByUsername("admin").orElseThrow(() -> new RuntimeException("User not found: admin"));
        return user.getId();
    }

    private void deleteCurrentUserTokens(UUID userId) {
        currentUserTokenRepository.deleteAllByUserId(userId);
    }

    @Transactional
    public boolean isAccountLocked(String username) {
        username = username.toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        if (user.getAccountLockedUntil() == null) {
            return false;
        }

        // Check if lockout period has expired
        if (user.getAccountLockedUntil().isAfter(Instant.now())) {
            return true;
        }

        // Lockout has expired, unlock the account
        unlockAccount(username);
        return false;
    }

    @Transactional
    public void incrementFailedLoginAttempts(String username) {
        username = username.toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        Integer currentAttempts = user.getFailedLoginAttempts();
        if (currentAttempts == null) {
            currentAttempts = 0;
        }
        currentAttempts++;

        user.setFailedLoginAttempts(currentAttempts);
        user.setLastFailedLoginAttempt(Instant.now());

        // Check if we need to lock the account
        Integer maxAttempts = securitySettingsService.getSecuritySettings().getMaximumLoginAttempts();
        if (maxAttempts != null && currentAttempts >= maxAttempts) {
            lockAccount(user);
        }

        userRepository.save(user);
    }

    @Transactional
    public void resetFailedLoginAttempts(String username) {
        username = username.toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAttempt(null);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
    }

    @Transactional
    public void lockAccount(User user) {
        Integer lockoutMinutes = securitySettingsService.getSecuritySettings().getAfterMaxAttemptsLockoutTimeMinutes();
        if (lockoutMinutes == null) {
            lockoutMinutes = 15; // Default 15 minutes
        }

        Instant lockUntil = Instant.now().plusSeconds(lockoutMinutes * 60L);
        user.setAccountLockedUntil(lockUntil);
        log.warn("Account locked for user: {} until {}", user.getUsername(), lockUntil);
    }

    @Transactional
    public void unlockAccount(String username) {
        username = username.toLowerCase();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return;
        }

        User user = userOpt.get();
        user.setFailedLoginAttempts(0);
        user.setLastFailedLoginAttempt(null);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        log.info("Account unlocked for user: {}", username);
    }

}
