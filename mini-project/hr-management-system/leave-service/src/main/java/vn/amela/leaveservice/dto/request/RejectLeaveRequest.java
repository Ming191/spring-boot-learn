package vn.amela.leaveservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLeaveRequest(
        @NotBlank(message = "Reviewer note cannot be blank")
        @Size(max = 500)
        String reviewerNote
) { }
