package org.ulinda.entities;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Table("model_links")
public class ModelLink {

    @Id
    private UUID id;

    @Column("model_1_id")
    private UUID model1Id;

    @Column("model_2_id")
    private UUID model2Id;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Column("model1_can_have_unlimited_model2s")
    private boolean model1CanHaveUnlimitedModel2s;

    @Column("model2_can_have_unlimited_model1s")
    private boolean model2CanHaveUnlimitedModel1s;

    @Column("model1_can_have_so_many_model2s_count")
    private Long model1CanHaveSoManyModel2sCount;

    @Column("model2_can_have_so_many_model1s_count")
    private Long model2CanHaveSoManyModel1sCount;

}
