package org.ulinda.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetAllTokensResponse {
    private List<AdminTokenDto> tokens;
    private TokenPagingInfo pagingInfo;
}
