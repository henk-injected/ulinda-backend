package org.ulinda.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class ModelLinkDto {
    private UUID modelLinkId;
    private UUID model1Id;
    private UUID model2Id;

    private boolean model1_can_have_unlimited_model2s;
    private boolean model2_can_have_unlimited_model1s;

    private Long model1_can_have_so_many_model2s_count;
    private Long model2_can_have_so_many_model1s_count;

    private String model1Name;
    private String model2Name;
}
