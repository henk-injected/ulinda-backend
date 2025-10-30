package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("password_history")
public class PasswordHistory {
    @Id
    private UUID id;
    private UUID userId;
    private String passwordHash;
    private Instant createdAt;
}
