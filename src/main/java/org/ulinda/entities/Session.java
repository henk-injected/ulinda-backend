package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("sessions")
public class Session {
    @Id
    private UUID id;
    private Instant createdAt;
    private Instant lastAccessed;
    private UUID userId;
    private String ipAddr;
}
