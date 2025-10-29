package org.ulinda.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class ErrorPagingInfo {
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;

    // Default constructor
    public ErrorPagingInfo() {}

    // Constructor from Spring's Page object
    public ErrorPagingInfo(Page<?> page) {
        this.currentPage = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
        this.hasNext = page.hasNext();
        this.hasPrevious = page.hasPrevious();
    }

    // Manual constructor for custom pagination
    public ErrorPagingInfo(int currentPage, int pageSize, long totalElements) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
        this.first = currentPage == 0;
        this.last = currentPage >= totalPages - 1;
        this.hasNext = currentPage < totalPages - 1;
        this.hasPrevious = currentPage > 0;
    }
}
