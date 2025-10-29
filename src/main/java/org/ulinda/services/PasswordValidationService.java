package org.ulinda.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ulinda.entities.SecuritySettings;
import org.ulinda.repositories.SecuritySettingsRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordValidationService {

    @Autowired
    private SecuritySettingsRepository securitySettingsRepository;

    public PasswordValidationResult validatePassword(String password) {
        SecuritySettings settings = securitySettingsRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("SecuritySettings not found"));

        List<String> errors = new ArrayList<>();

        // Check minimum length
        if (password.length() < settings.getMinimumPasswordLength()) {
            errors.add("Password must be at least " + settings.getMinimumPasswordLength() + " characters long");
        }

        // Check uppercase letters
        if (settings.getPasswordRequiresUppercaseLetters()) {
            long uppercaseCount = password.chars().filter(Character::isUpperCase).count();
            if (uppercaseCount < settings.getPasswordRequiresUppercaseLettersMinimumCount()) {
                errors.add("Password must contain at least " + settings.getPasswordRequiresUppercaseLettersMinimumCount() + " uppercase letter(s)");
            }
        }

        // Check lowercase letters
        if (settings.getPasswordRequiresLowercaseLetters()) {
            long lowercaseCount = password.chars().filter(Character::isLowerCase).count();
            if (lowercaseCount < settings.getPasswordRequiresLowercaseLettersMinimumCount()) {
                errors.add("Password must contain at least " + settings.getPasswordRequiresLowercaseLettersMinimumCount() + " lowercase letter(s)");
            }
        }

        // Check numbers
        if (settings.getPasswordRequiresNumbers()) {
            long numberCount = password.chars().filter(Character::isDigit).count();
            if (numberCount < settings.getPasswordRequiresNumbersMinimumCount()) {
                errors.add("Password must contain at least " + settings.getPasswordRequiresNumbersMinimumCount() + " number(s)");
            }
        }

        // Check special characters
        if (settings.getPasswordRequiresSpecialCharacters()) {
            String allowedSpecialChars = settings.getPasswordAllowedSpecialCharacters();
            long specialCharCount = password.chars()
                    .filter(c -> allowedSpecialChars.indexOf(c) >= 0)
                    .count();
            if (specialCharCount < settings.getPasswordRequiresSpecialCharactersMinimumCount()) {
                errors.add("Password must contain at least " + settings.getPasswordRequiresSpecialCharactersMinimumCount() +
                          " special character(s) from: " + allowedSpecialChars);
            }
        }

        return new PasswordValidationResult(errors.isEmpty(), errors);
    }

    public PasswordValidationResult validatePasswordWithUsername(String password, String username) {
        PasswordValidationResult result = validatePassword(password);

        SecuritySettings settings = securitySettingsRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("SecuritySettings not found"));

        // Check if password contains username
        if (settings.getPreventUsernameInPassword() && username != null) {
            if (password.toLowerCase().contains(username.toLowerCase())) {
                result.getErrors().add("Password must not contain your username");
                result.setValid(false);
            }
        }

        return result;
    }

    public static class PasswordValidationResult {
        private boolean valid;
        private List<String> errors;

        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
