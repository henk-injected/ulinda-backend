package org.ulinda.dto;

import lombok.Data;
import java.util.List;

@Data
public class GetRecordsResponse {
    private List<RecordDto> records;
    private PaginationInfo pagination;
    private List<FieldDto> fields; // Field metadata for the frontend
}