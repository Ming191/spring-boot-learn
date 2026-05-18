package vn.amela.employeeservice.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        int totalElements,
        int totalPages
) {}
