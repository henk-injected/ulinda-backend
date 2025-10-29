package org.ulinda.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
@Getter
@Setter
public class User {
    @Id
    private UUID id;
    private String username;
    private String password;
    private String name;
    private String surname;
    private boolean canCreateModels;
    @Column("is_admin_user")
    private boolean adminUser;
    private boolean mustChangePassword;
    private boolean canGenerateTokens;
    private Integer maxTokenCount;
    private boolean accountDisabled;
    private Integer failedLoginAttempts;
    private Instant lastFailedLoginAttempt;
    private Instant accountLockedUntil;

    // Constructors
    public User() {}

    public User(String username,
                String password,
                String name,
                String surname ,
                boolean canCreateModels,
                boolean adminUser,
                boolean canGenerateTokens,
                Integer maxTokenCount) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.canCreateModels = canCreateModels;
        this.adminUser = adminUser;
        this.canGenerateTokens = canGenerateTokens;
        this.maxTokenCount = maxTokenCount;
    }
}
