package org.ulinda.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetUserTokensResponse {
    private List<UserTokenDto> tokens;
}
