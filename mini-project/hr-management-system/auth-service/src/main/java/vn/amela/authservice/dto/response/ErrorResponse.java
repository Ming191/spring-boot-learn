package vn.amela.authservice.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private List<FieldErrorResponse> errors;
}
