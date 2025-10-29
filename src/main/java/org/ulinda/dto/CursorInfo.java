package org.ulinda.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.UUID;

/**
 * Represents cursor information for pagination.
 * Contains both the sort field value and record ID for proper tie-breaking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CursorInfo {
    private String sortFieldValue; // The value of the field we're sorting by (as string)
    private UUID recordId;        // The ID of the record (tie-breaker)
    private String sortField;     // The field name/column we're sorting by
    private String sortOrder;     // ASC or DESC
}