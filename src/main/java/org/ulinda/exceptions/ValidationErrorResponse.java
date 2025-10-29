package org.ulinda.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ValidationErrorResponse extends ErrorResponse {
    private Map<String, String> fieldErrors;
}
