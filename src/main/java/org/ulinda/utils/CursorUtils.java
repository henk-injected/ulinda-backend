package org.ulinda.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ulinda.dto.CursorInfo;
import org.ulinda.dto.RecordDto;
import org.ulinda.entities.Field;
import org.ulinda.enums.FieldType;
import org.ulinda.exceptions.FrontendException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for handling cursor-based pagination.
 * Provides encoding/decoding of cursor information for proper pagination.
 */
@Slf4j
public class CursorUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Encodes cursor information into a Base64 string.
     */
    public static String encodeCursor(CursorInfo cursorInfo) {
        try {
            String json = objectMapper.writeValueAsString(cursorInfo);
            return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to encode cursor: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decodes a Base64 cursor string back to CursorInfo.
     */
    public static CursorInfo decodeCursor(String cursor) {
        try {
            if (cursor == null || cursor.trim().isEmpty()) {
                return null;
            }
            
            byte[] decodedBytes = Base64.getDecoder().decode(cursor);
            String json = new String(decodedBytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, CursorInfo.class);
        } catch (Exception e) {
            log.error("Failed to decode cursor '{}': {}", cursor, e.getMessage());
            return null;
        }
    }

    /**
     * Creates cursor info from a record and sort parameters.
     */
    public static CursorInfo createCursorFromRecord(RecordDto record, String sortField, String sortOrder, List<Field> fields) {
        try {
            String sortFieldValue = extractSortFieldValue(record, sortField, fields);
            return new CursorInfo(sortFieldValue, record.getId(), sortField, sortOrder);
        } catch (Exception e) {
            log.error("Failed to create cursor from record {}: {}", record.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the sort field value from a record as a string.
     */
    private static String extractSortFieldValue(RecordDto record, String sortField, List<Field> fields) {
        // Handle system fields
        if ("id".equals(sortField)) {
            return record.getId().toString();
        }
        if ("created_at".equals(sortField)) {
            return record.getCreatedAt() != null ? record.getCreatedAt().toString() : "";
        }
        if ("updated_at".equals(sortField)) {
            return record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : "";
        }

        // Handle custom fields - sortField should be the field UUID
        try {
            UUID fieldId = UUID.fromString(sortField);
            Object value = record.getFieldValues().get(fieldId);
            return value != null ? value.toString() : "";
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sort field UUID: {}", sortField);
            return "";
        }
    }

    /**
     * Converts a string value to the appropriate type for database comparison.
     */
    public static Object convertCursorValueForComparison(String stringValue, String sortField, List<Field> fields) {
        if (stringValue == null) {
            throw new FrontendException("Sort value is null", true);
        }

        try {
            // Handle system fields
            if ("id".equals(sortField)) {
                return UUID.fromString(stringValue);
            }
            if ("created_at".equals(sortField) || "updated_at".equals(sortField)) {
                return java.sql.Timestamp.from(java.time.Instant.parse(stringValue));
            }

            // Handle custom fields
            try {
                UUID fieldId = UUID.fromString(sortField);
                Field field = fields.stream()
                        .filter(f -> f.getId().equals(fieldId))
                        .findFirst()
                        .orElse(null);

                if (field == null) {
                    throw new FrontendException("Field not found for sorting : " + sortField, false);
                }
                return convertStringToFieldType(stringValue, field.getType());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid field UUID: {}", sortField);
                throw new FrontendException("Invalid sort field UUID: " + sortField, false);
            }
        } catch (Exception e) {
            log.warn("Could not convert cursor value '{}' for sort field '{}': {}", stringValue, sortField, e.getMessage());
            throw new FrontendException("Could not convert cursor value for sort field: " + sortField, false);
        }
    }

    /**
     * Converts a string value to the appropriate field type.
     */
    private static Object convertStringToFieldType(String value, FieldType fieldType) {

        //Check if value is not "" for specific field types.
        switch (fieldType) {
            case BOOLEAN:
            case DECIMAL:
            case DATE:
            case DATETIME:
            case LONG:
                if (value.equals("")) {
                    throw new RuntimeException("Invalid: String for type " + fieldType + " is empty.");
                }
        }

        return switch (fieldType) {
            case EMAIL, SINGLE_LINE_TEXT, MULTI_LINE_TEXT -> value;
            case DECIMAL -> new java.math.BigDecimal(value);
            case LONG -> Long.valueOf(value);
            case BOOLEAN -> Boolean.parseBoolean(value);
            case DATE -> java.sql.Date.valueOf(value);
            case DATETIME -> java.sql.Timestamp.from(java.time.Instant.parse(value));
        };
    }

    /**
     * Gets the database column name for a sort field.
     */
    public static String getDatabaseColumnName(String sortField, List<Field> fields) {
        // Handle system fields
        if ("id".equals(sortField) || "created_at".equals(sortField) || "updated_at".equals(sortField)) {
            return sortField;
        }

        // Handle custom fields
        try {
            UUID fieldId = UUID.fromString(sortField);
            if (fields.stream().anyMatch(f -> f.getId().equals(fieldId))) {
                return "\"field_" + sanitizeIdentifier(fieldId.toString()) + "\"";
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid field UUID: {}", sortField);
        }

        log.warn("Unknown sort field: {}, defaulting to id", sortField);
        return "id";
    }

    private static String sanitizeIdentifier(String identifier) {
        return identifier.replaceAll("-", "_").replaceAll("[^a-zA-Z0-9_]", "");
    }
}