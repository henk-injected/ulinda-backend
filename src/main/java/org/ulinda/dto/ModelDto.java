package org.ulinda.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class ModelDto {
    private UUID id;
    private String name;
    private String description;

    private List<FieldDto> fields = new ArrayList<>();
}
