package org.ulinda.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetModelResponse {
    private ModelDto model;
    private List<ModelLinkTarget> modelLinkTargets = new ArrayList<>();
}
