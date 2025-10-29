package org.ulinda.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GetUsersResponse {
    private List<UserDto> users = new ArrayList<>();
}
