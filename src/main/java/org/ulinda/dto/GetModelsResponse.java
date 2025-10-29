package org.ulinda.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetModelsResponse {
    List<ModelDto> models =  new ArrayList<>();
}
