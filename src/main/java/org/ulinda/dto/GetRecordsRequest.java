package org.ulinda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ulinda.enums.QueryType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class GetRecordsRequest {
    private int limit = 20;
    private String cursor;
    private String sortField;
    private String sortOrder = "asc";
    private boolean previous = false;
    @Valid
    private List<SearchParameter>  searchParameters = new ArrayList<>();
    @NotNull
    private QueryType queryType;
    private UUID modelLinkId;
    private UUID sourceRecordId;
}


