package org.ulinda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SearchParameter {
    private UUID fieldID;
    @NotNull
    private SearchFieldIdentifier searchFieldIdentifier;
    @NotNull
    private SearchType searchType;
    private String textSearchValue;
    private LocalDate dateOn;
    private LocalDate dateBefore;
    private LocalDate dateAfter;
    private LocalDate dateStart;
    private LocalDate dateEnd;
    private LocalDateTime dateTimeBefore;
    private LocalDateTime dateTimeAfter;
    private LocalDateTime dateTimeStart;
    private LocalDateTime dateTimeEnd;
    private Boolean yesNo;
    private Long longSearchValue;
    private BigDecimal doubleSearchValue;
}
