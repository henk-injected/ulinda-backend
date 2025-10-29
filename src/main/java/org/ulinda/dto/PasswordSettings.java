package org.ulinda.dto;

import lombok.Data;

@Data
public class PasswordSettings {
    private int minimumPasswordLength;
    private boolean passwordRequiresUppercaseLetters;
    private int passwordRequiresUppercaseLettersMinimumCount;
    private boolean passwordRequiresLowercaseLetters;
    private int passwordRequiresLowercaseLettersMinimumCount;
    private boolean passwordRequiresNumbers;
    private int passwordRequiresNumbersMinimumCount;
    private boolean passwordRequiresSpecialCharacters;
    private int passwordRequiresSpecialCharactersMinimumCount;
    private String passwordAllowedSpecialCharacters;
    private boolean showPasswordStrengthMeter;
    private boolean showPasswordRequirmentsOnForm;
    private boolean allowShowPasswordToggle;
}
