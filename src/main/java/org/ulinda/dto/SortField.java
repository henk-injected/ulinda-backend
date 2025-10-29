package org.ulinda.dto;

import lombok.Data;
import org.ulinda.enums.FieldType;

@Data
public class SortField {
    private String columnName;
    private String sortField;
    private FieldType fieldType;
}
