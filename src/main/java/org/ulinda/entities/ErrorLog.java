package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.ulinda.exceptions.ErrorCode;

import java.time.Instant;
import java.util.UUID;

@Data
@Table("error_logs")
public class ErrorLog {
    @Id
    private UUID id;

    @Column("error_identifier")
    private UUID errorIdentifier;

    @Column("timestamp")
    private Instant timestamp = Instant.now();

    @Column("message")
    private String message;

    @Column("stack_trace")
    private String stackTrace;

    @Column("error_code")
    private ErrorCode errorCode;
}
