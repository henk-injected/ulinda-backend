package org.ulinda.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.ulinda.dto.PasswordSettings;
import org.ulinda.dto.SecuritySettingsDto;
import org.ulinda.entities.SecuritySettings;
import org.ulinda.repositories.SecuritySettingsRepository;

@Service
public class SecuritySettingsService {

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int getSessionTimeoutMinutes() {
        SecuritySettings securitySettings = securitySettingsRepository.findById(1).orElseThrow(()->new RuntimeException("SecuritySettings not found"));
        return securitySettings.getSessionTimeoutMinutes();
    }

    public SecuritySettingsDto getSecuritySettings() {
        SecuritySettings securitySettings = securitySettingsRepository.findById(1).orElseThrow(()->new RuntimeException("SecuritySettings not found"));
        SecuritySettingsDto securitySettingsDto = new SecuritySettingsDto();
        securitySettingsDto.setId(securitySettings.getId());
        securitySettingsDto.setSessionTimeoutMinutes(securitySettings.getSessionTimeoutMinutes());
        securitySettingsDto.setMinimumPasswordLength(securitySettings.getMinimumPasswordLength());
        securitySettingsDto.setPasswordRequiresUppercaseLetters(securitySettings.getPasswordRequiresUppercaseLetters());
        securitySettingsDto.setPasswordRequiresUppercaseLettersMinimumCount(securitySettings.getPasswordRequiresUppercaseLettersMinimumCount());
        securitySettingsDto.setPasswordRequiresLowercaseLetters(securitySettings.getPasswordRequiresLowercaseLetters());
        securitySettingsDto.setPasswordRequiresLowercaseLettersMinimumCount(securitySettings.getPasswordRequiresLowercaseLettersMinimumCount());
        securitySettingsDto.setPasswordRequiresNumbers(securitySettings.getPasswordRequiresNumbers());
        securitySettingsDto.setPasswordRequiresNumbersMinimumCount(securitySettings.getPasswordRequiresNumbersMinimumCount());
        securitySettingsDto.setPasswordRequiresSpecialCharacters(securitySettings.getPasswordRequiresSpecialCharacters());
        securitySettingsDto.setPasswordRequiresSpecialCharactersMinimumCount(securitySettings.getPasswordRequiresSpecialCharactersMinimumCount());
        securitySettingsDto.setPasswordAllowedSpecialCharacters(securitySettings.getPasswordAllowedSpecialCharacters());
        securitySettingsDto.setPreventUsernameInPassword(securitySettings.getPreventUsernameInPassword());
        securitySettingsDto.setPasswordExpiration(securitySettings.getPasswordExpiration());
        securitySettingsDto.setPasswordExpirationDays(securitySettings.getPasswordExpirationDays());
        securitySettingsDto.setRememberPreviousPasswords(securitySettings.getRememberPreviousPasswords());
        securitySettingsDto.setRememberLastPasswordsCount(securitySettings.getRememberLastPasswordsCount());
        securitySettingsDto.setMaximumLoginAttempts(securitySettings.getMaximumLoginAttempts());
        securitySettingsDto.setAfterMaxAttemptsLockoutTimeMinutes(securitySettings.getAfterMaxAttemptsLockoutTimeMinutes());
        securitySettingsDto.setShowPasswordStrengthMeter(securitySettings.getShowPasswordStrengthMeter());
        securitySettingsDto.setShowPasswordRequirmentsOnForm(securitySettings.getShowPasswordRequirmentsOnForm());
        securitySettingsDto.setAllowShowPasswordToggle(securitySettings.getAllowShowPasswordToggle());
        return securitySettingsDto;
    }

    public PasswordSettings getPasswordSettings() {
        SecuritySettings securitySettings = securitySettingsRepository.findById(1).orElseThrow(()->new RuntimeException("SecuritySettings not found"));
        PasswordSettings passwordSettings = new PasswordSettings();
        passwordSettings.setMinimumPasswordLength(securitySettings.getMinimumPasswordLength());
        passwordSettings.setPasswordRequiresUppercaseLetters(securitySettings.getPasswordRequiresUppercaseLetters());
        passwordSettings.setPasswordRequiresUppercaseLettersMinimumCount(securitySettings.getPasswordRequiresUppercaseLettersMinimumCount());
        passwordSettings.setPasswordRequiresLowercaseLetters(securitySettings.getPasswordRequiresLowercaseLetters());
        passwordSettings.setPasswordRequiresLowercaseLettersMinimumCount(securitySettings.getPasswordRequiresLowercaseLettersMinimumCount());
        passwordSettings.setPasswordRequiresNumbers(securitySettings.getPasswordRequiresNumbers());
        passwordSettings.setPasswordRequiresNumbersMinimumCount(securitySettings.getPasswordRequiresNumbersMinimumCount());
        passwordSettings.setPasswordRequiresSpecialCharacters(securitySettings.getPasswordRequiresSpecialCharacters());
        passwordSettings.setPasswordRequiresSpecialCharactersMinimumCount(securitySettings.getPasswordRequiresSpecialCharactersMinimumCount());
        passwordSettings.setPasswordAllowedSpecialCharacters(securitySettings.getPasswordAllowedSpecialCharacters());
        passwordSettings.setShowPasswordStrengthMeter(securitySettings.getShowPasswordStrengthMeter());
        passwordSettings.setShowPasswordRequirmentsOnForm(securitySettings.getShowPasswordRequirmentsOnForm());
        passwordSettings.setAllowShowPasswordToggle(securitySettings.getAllowShowPasswordToggle());
        return passwordSettings;
    }

    public void updateSecuritySettings(SecuritySettingsDto securitySettingsDto) {
        SecuritySettings securitySettings = securitySettingsRepository.findById(1).orElseThrow(()->new RuntimeException("SecuritySettings not found"));
        securitySettings.setSessionTimeoutMinutes(securitySettingsDto.getSessionTimeoutMinutes());
        securitySettings.setMinimumPasswordLength(securitySettingsDto.getMinimumPasswordLength());
        securitySettings.setPasswordRequiresUppercaseLetters(securitySettingsDto.getPasswordRequiresUppercaseLetters());
        securitySettings.setPasswordRequiresUppercaseLettersMinimumCount(securitySettingsDto.getPasswordRequiresUppercaseLettersMinimumCount());
        securitySettings.setPasswordRequiresLowercaseLetters(securitySettingsDto.getPasswordRequiresLowercaseLetters());
        securitySettings.setPasswordRequiresLowercaseLettersMinimumCount(securitySettingsDto.getPasswordRequiresLowercaseLettersMinimumCount());
        securitySettings.setPasswordRequiresNumbers(securitySettingsDto.getPasswordRequiresNumbers());
        securitySettings.setPasswordRequiresNumbersMinimumCount(securitySettingsDto.getPasswordRequiresNumbersMinimumCount());
        securitySettings.setPasswordRequiresSpecialCharacters(securitySettingsDto.getPasswordRequiresSpecialCharacters());
        securitySettings.setPasswordRequiresSpecialCharactersMinimumCount(securitySettingsDto.getPasswordRequiresSpecialCharactersMinimumCount());
        securitySettings.setPasswordAllowedSpecialCharacters(securitySettingsDto.getPasswordAllowedSpecialCharacters());
        securitySettings.setPreventUsernameInPassword(securitySettingsDto.getPreventUsernameInPassword());
        securitySettings.setPasswordExpiration(securitySettingsDto.getPasswordExpiration());
        securitySettings.setPasswordExpirationDays(securitySettingsDto.getPasswordExpirationDays());
        securitySettings.setRememberPreviousPasswords(securitySettingsDto.getRememberPreviousPasswords());
        securitySettings.setRememberLastPasswordsCount(securitySettingsDto.getRememberLastPasswordsCount());
        securitySettings.setMaximumLoginAttempts(securitySettingsDto.getMaximumLoginAttempts());
        securitySettings.setAfterMaxAttemptsLockoutTimeMinutes(securitySettingsDto.getAfterMaxAttemptsLockoutTimeMinutes());
        securitySettings.setShowPasswordStrengthMeter(securitySettingsDto.getShowPasswordStrengthMeter());
        securitySettings.setShowPasswordRequirmentsOnForm(securitySettingsDto.getShowPasswordRequirmentsOnForm());
        securitySettings.setAllowShowPasswordToggle(securitySettingsDto.getAllowShowPasswordToggle());
        securitySettingsRepository.save(securitySettings);
    }

    public void saveNewSecuritySettings() {
        String insertSql = """
            INSERT INTO security_settings (
                id,
                session_timeout_minutes,
                minimum_password_length,
                password_requires_uppercase_letters,
                password_requires_uppercase_letters_minimum_count,
                password_requires_lowercase_letters,
                password_requires_lowercase_letters_minimum_count,
                password_requires_numbers,
                password_requires_numbers_minimum_count,
                password_requires_special_characters,
                password_requires_special_characters_minimum_count,
                password_allowed_special_characters,
                prevent_username_in_password,
                password_expiration,
                password_expiration_days,
                remember_previous_passwords,
                remember_last_passwords_count,
                maximum_login_attempts,
                after_max_attempts_lockout_time_minutes,
                show_password_strength_meter,
                show_password_requirments_on_form,
                allow_show_password_toggle
            ) VALUES (
                1,
                30,
                8,
                true,
                1,
                true,
                1,
                true,
                1,
                true,
                1,
                '!@#$%^&*()_+-=[]{}|;:,.<>?',
                true,
                false,
                90,
                true,
                5,
                5,
                15,
                true,
                true,
                true
            )
        """;
        jdbcTemplate.execute(insertSql);
    }
}
