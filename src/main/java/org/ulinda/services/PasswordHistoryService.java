package org.ulinda.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ulinda.entities.PasswordHistory;
import org.ulinda.entities.SecuritySettings;
import org.ulinda.repositories.PasswordHistoryRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySettingsService securitySettingsService;

    public PasswordHistoryService(
            PasswordHistoryRepository passwordHistoryRepository,
            PasswordEncoder passwordEncoder,
            SecuritySettingsService securitySettingsService) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.securitySettingsService = securitySettingsService;
    }

    /**
     * Checks if the given plain text password matches any password in the user's history
     * @param userId The user's ID
     * @param plainTextPassword The plain text password to check
     * @return true if password was used before, false otherwise
     */
    public boolean isPasswordInHistory(UUID userId, String plainTextPassword) {
        SecuritySettings settings = securitySettingsService.getSecuritySettings();

        // If password history is not enabled, allow any password
        if (!settings.getRememberPreviousPasswords()) {
            return false;
        }

        // Get password history for user, ordered by most recent first
        List<PasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Check if plain text password matches any stored hash
        for (PasswordHistory entry : history) {
            if (passwordEncoder.matches(plainTextPassword, entry.getPasswordHash())) {
                log.debug("Password matches history entry from {}", entry.getCreatedAt());
                return true;
            }
        }

        return false;
    }

    /**
     * Adds a password hash to the user's history and maintains the history limit
     * @param userId The user's ID
     * @param passwordHash The BCrypt hash to store
     */
    @Transactional
    public void addPasswordToHistory(UUID userId, String passwordHash) {
        SecuritySettings settings = securitySettingsService.getSecuritySettings();

        // If password history is not enabled, don't store anything
        if (!settings.getRememberPreviousPasswords()) {
            return;
        }

        // Create new history entry
        PasswordHistory entry = new PasswordHistory();
        entry.setUserId(userId);
        entry.setPasswordHash(passwordHash);
        entry.setCreatedAt(Instant.now());
        passwordHistoryRepository.save(entry);

        // Maintain history limit
        maintainHistoryLimit(userId, settings.getRememberLastPasswordsCount());
    }

    /**
     * Ensures the user's password history doesn't exceed the configured limit
     * Deletes oldest entries if limit is exceeded
     */
    @Transactional
    public void maintainHistoryLimit(UUID userId, int maxHistoryCount) {
        List<PasswordHistory> history = passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // If we have more entries than the limit, delete the oldest ones
        if (history.size() > maxHistoryCount) {
            List<PasswordHistory> entriesToDelete = history.subList(maxHistoryCount, history.size());
            passwordHistoryRepository.deleteAll(entriesToDelete);
            log.debug("Deleted {} old password history entries for user {}", entriesToDelete.size(), userId);
        }
    }

    /**
     * Deletes all password history for a user
     * @param userId The user's ID
     */
    @Transactional
    public void deleteHistoryForUser(UUID userId) {
        passwordHistoryRepository.deleteByUserId(userId);
        log.debug("Deleted all password history for user {}", userId);
    }
}
