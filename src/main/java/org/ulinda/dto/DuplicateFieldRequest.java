package org.ulinda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.ulinda.enums.FieldType;

@Data
public class DuplicateFieldRequest {

    @NotNull(message = "New field name is required")
    @Size(min = 1, max = 100, message = "Field name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_\\s]*$", message = "Field name must start with a letter and contain only letters, numbers, spaces, and underscores")
    private String newFieldName;

    /**
     * The type for the new field. Can be different from the original field type.
     * If null, will use the same type as the original field.
     *
     * Allowed conversions:
     * - Any type -> SINGLE_LINE_TEXT (all data converted to text)
     * - Any type -> MULTI_LINE_TEXT (all data converted to text)
     * - SINGLE_LINE_TEXT -> LONG (only valid integers copied)
     * - SINGLE_LINE_TEXT -> DECIMAL (only valid numbers copied)
     * - LONG -> DECIMAL (all data copied)
     * - Same type -> Same type (always allowed)
     */
    private FieldType newFieldType;

    /**
     * Whether to copy data from the original field to the new field.
     * Data will be converted to the new type if necessary.
     * Default: true
     */
    private boolean copyData = true;
}
