package org.ulinda.entities;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("models")
@Data
public class Model {

    @Id
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("owner_id")
    private UUID ownerId;

}
