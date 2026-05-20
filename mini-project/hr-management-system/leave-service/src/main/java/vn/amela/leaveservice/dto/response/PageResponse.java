package vn.amela.leaveservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int size;
    private int page;
    private int totalPages;
    private Long totalElements;
}
