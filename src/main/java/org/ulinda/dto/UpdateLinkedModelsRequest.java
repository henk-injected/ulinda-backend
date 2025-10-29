package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateLinkedModelsRequest {
    @NotNull
    private UUID modelLinkId;

    private boolean model1_can_have_unlimited_model2s;
    private boolean model2_can_have_unlimited_model1s;

    private Long model1_can_have_so_many_model2s_count;
    private Long model2_can_have_so_many_model1s_count;
}
