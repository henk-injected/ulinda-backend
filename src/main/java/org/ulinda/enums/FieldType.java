package org.ulinda.enums;

// Enum for field types
public enum FieldType {
    SINGLE_LINE_TEXT,  // For single line text input
    MULTI_LINE_TEXT,   // For multi line text input
    DECIMAL,        // NUMERIC(15,4) - handles decimal numbers
    LONG,           // BIGINT - handles large integers
    BOOLEAN,
    DATE,
    DATETIME,
    EMAIL
}