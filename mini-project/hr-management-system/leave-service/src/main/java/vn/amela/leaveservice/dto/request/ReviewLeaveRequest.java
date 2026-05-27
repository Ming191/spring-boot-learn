package vn.amela.leaveservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReviewLeaveRequest(
        @Size(max = 500)
        String reviewerNote
) { }
