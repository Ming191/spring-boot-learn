package vn.amela.webservice.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    Long totalElements,
    int totalPages
) {
    public PageResponse {
        items = items == null ? List.of() : List.copyOf(items);
        totalElements = totalElements == null ? 0L : totalElements;
    }
}
