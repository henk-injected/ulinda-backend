package org.ulinda.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.ulinda.dto.*;
import org.ulinda.entities.ErrorLog;
import org.ulinda.entities.SecuritySettings;
import org.ulinda.services.ErrorService;
import org.ulinda.services.SecuritySettingsService;
import org.ulinda.services.TokenService;
import org.ulinda.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ErrorService errorService;

    @Autowired
    private SecuritySettingsService securitySettingsService;

    @Autowired
    private TokenService tokenService;

    @GetMapping("/users")
    public ResponseEntity<GetUsersResponse> getUsers() {
        GetUsersResponse response = new GetUsersResponse();
        response.setUsers(userService.getUsers());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        return ResponseEntity.ok(userService.createUser(createUserRequest));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<GetUserResponse> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @GetMapping("/user/model-permissions/{userId}")
    public ResponseEntity<GetUserModelPermissionsResponse> getUserModelPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserModelPermissions(userId));
    }

    @PostMapping("/user/model-permissions")
    public void updateUser(@Valid @RequestBody UpdateUserRequest updateUserRequest) {
        userService.updateUser(updateUserRequest);
    }

    @GetMapping("/user/reset-password/{userId}")
    public ResponseEntity<UserResetPasswordResponse> resetPassword(@PathVariable UUID userId) {
        UserResetPasswordResponse response = new UserResetPasswordResponse();
        response.setNewPassword(userService.resetPassword(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/errors")
    public ResponseEntity<GetErrorsResponse> getErrors(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<ErrorLog> errors = errorService.getErrors(pageable);

        GetErrorsResponse response = new GetErrorsResponse();

        // Convert ErrorLog entities to ErrorDto
        List<ErrorDto> errorDtos = errors.getContent()
                .stream()
                .map(this::convertToDto)  // or use a mapper service
                .collect(Collectors.toList());

        // Set the converted errors
        response.setErrors(errorDtos);

        // Set pagination info
        response.setPagingInfo(new ErrorPagingInfo(errors));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/error/{errorIdentifier}")
    public ResponseEntity<ErrorDetailDto> getError(
            @PathVariable UUID errorIdentifier) {
        return ResponseEntity.ok(errorService.getErrorDetail(errorIdentifier));
    }

    @GetMapping("/security-settings")
    public ResponseEntity<SecuritySettingsDto> getSecuritySettings() {
        return ResponseEntity.ok(securitySettingsService.getSecuritySettings());
    }

    @PutMapping("/security-settings")
    public void updateSecuritySettings(@Valid @RequestBody SecuritySettingsDto securitySettingsDto) {
        securitySettingsService.updateSecuritySettings(securitySettingsDto);
    }

    @GetMapping("/tokens")
    public ResponseEntity<GetAllTokensResponse> getAllTokens(
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = Sort.Direction.fromString(sortDirection);
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(pageNumber, size, sort);
        GetAllTokensResponse response = tokenService.getAllTokens(pageable);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/tokens/{tokenId}")
    public ResponseEntity<Void> deleteToken(@PathVariable UUID tokenId) {
        tokenService.deleteTokenAdmin(tokenId);
        return ResponseEntity.noContent().build();
    }

    // Helper method to convert ErrorLog to ErrorDto
    private ErrorDto convertToDto(ErrorLog errorLog) {
        ErrorDto dto = new ErrorDto();
        dto.setErrorIdentifier(errorLog.getErrorIdentifier());
        dto.setMessage(errorLog.getMessage());
        dto.setTimestamp(errorLog.getTimestamp());
        // Map other fields as needed
        return dto;
    }


}



