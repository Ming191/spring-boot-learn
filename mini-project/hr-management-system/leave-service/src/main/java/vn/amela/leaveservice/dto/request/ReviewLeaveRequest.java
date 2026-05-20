package vn.amela.leaveservice.dto.request;

import lombok.Builder;

@Builder
public record ReviewLeaveRequest(
        String reviewerNote
) { }
