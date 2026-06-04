package vn.amela.leaveservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageResponse<T> {
    private List<T> items;
    private Integer size;
    private Integer page;
    private Integer totalPages;
    private Long totalElements;
}
