package org.ulinda.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import org.ulinda.enums.FieldType;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table("fields")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Field {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @Column("name")
    private String name;

    @Column("model_id")
    private UUID modelId;

    @Column("description")
    private String description;

    @Column("type")
    private FieldType type;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("is_required")
    private Boolean isRequired;

    // Constructor for creating new fields (without ID and timestamps)
    public Field(String name, String description, FieldType type) {
        this.name = name;
        this.description = description;
        this.type = type;
    }

    // Constructor without description (for required fields only)
    public Field(String name, FieldType type) {
        this.name = name;
        this.type = type;
    }
}


