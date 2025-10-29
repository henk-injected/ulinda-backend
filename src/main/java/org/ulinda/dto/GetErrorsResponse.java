package org.ulinda.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetErrorsResponse {
    private List<ErrorDto> errors;
    private ErrorPagingInfo pagingInfo;
}
