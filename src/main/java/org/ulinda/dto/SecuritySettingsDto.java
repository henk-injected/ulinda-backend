package org.ulinda.dto;

import lombok.Data;

@Data
public class SecuritySettingsDto {
    private Integer id;
    private Integer sessionTimeoutMinutes;
    private Integer minimumPasswordLength;
    private Boolean passwordRequiresUppercaseLetters;
    private Integer passwordRequiresUppercaseLettersMinimumCount;
    private Boolean passwordRequiresLowercaseLetters;
    private Integer passwordRequiresLowercaseLettersMinimumCount;
    private Boolean passwordRequiresNumbers;
    private Integer passwordRequiresNumbersMinimumCount;
    private Boolean passwordRequiresSpecialCharacters;
    private Integer passwordRequiresSpecialCharactersMinimumCount;
    private String passwordAllowedSpecialCharacters;
    private Boolean blockCommonPasswords;
    private Boolean blockDictionaryWords;
    private Boolean preventUsernameInPassword;
    private Boolean passwordExpiration;
    private Integer passwordExpirationDays;
    private Boolean rememberPreviousPasswords;
    private Integer rememberLastPasswordsCount;
    private Integer maximumLoginAttempts;
    private Integer afterMaxAttemptsLockoutTimeMinutes;
    private Boolean showPasswordStrengthMeter;
    private Boolean showPasswordRequirmentsOnForm;
    private Boolean allowShowPasswordToggle;
}
