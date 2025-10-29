package org.ulinda.enums;

public enum SearchFieldType {

    UUID,
    TEXT,
    DECIMAL,
    LONG,
    BOOLEAN,
    DATE,
    DATETIME;

    public static SearchFieldType fromFieldType(FieldType fieldType) throws IllegalArgumentException {
        return switch (fieldType) {
            case MULTI_LINE_TEXT, SINGLE_LINE_TEXT, EMAIL -> SearchFieldType.TEXT;
            case BOOLEAN ->  SearchFieldType.BOOLEAN;
            case DATE ->  SearchFieldType.DATE;
            case DATETIME ->  SearchFieldType.DATETIME;
            case DECIMAL ->   SearchFieldType.DECIMAL;
            case LONG ->  SearchFieldType.LONG;
        };
    }

}
