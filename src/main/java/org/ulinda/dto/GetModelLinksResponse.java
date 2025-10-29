package org.ulinda.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetModelLinksResponse {
    private List<ModelLinkDto> modelLinks;
}
