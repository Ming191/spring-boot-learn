package vn.amela.webservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RejectLeaveForm(
    @NotBlank(message = "Reviewer note cannot be blank")
    @Size(max = 500, message = "Reviewer note must be less than or equal to 500 characters")
    String reviewerNote
) {}
