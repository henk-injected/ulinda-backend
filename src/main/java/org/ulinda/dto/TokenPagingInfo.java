package org.ulinda.dto;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.ulinda.entities.CurrentUserToken;

@Data
public class TokenPagingInfo {
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    public TokenPagingInfo(Page<CurrentUserToken> page) {
        this.currentPage = page.getNumber();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        this.pageSize = page.getSize();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }
}
