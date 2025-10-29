package org.ulinda.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateModelRequest {
    String name;
    String description;

    List<FieldDto> fields = new ArrayList<>();
}
