package org.ulinda.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class PaginationInfo {
    private int limit;
    private boolean hasNext;
    private boolean hasPrevious;
    private String nextCursor; // Changed from UUID to String
    private String previousCursor; // Changed from UUID to String
    private long totalEstimate;
    private String sortField;
    private String sortOrder;
    private long actualRecordCount;
}