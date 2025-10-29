package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Table("current_user_tokens")
public class CurrentUserToken {
    @Id
    private UUID id;
    private UUID userId;
    private String tokenHash;
    private String tokenPrefix;
    private String tokenName;
    private Instant createdAt;
    private Instant tokenExpiryDateTime;
}
