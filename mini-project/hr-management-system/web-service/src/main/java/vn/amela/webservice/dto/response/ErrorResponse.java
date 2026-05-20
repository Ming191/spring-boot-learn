package vn.amela.webservice.dto.response;

import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorResponse> errors
) {
    public ErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
