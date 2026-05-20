package vn.amela.webservice.dto.response;

import lombok.Builder;

@Builder
public record FieldErrorResponse(
    String field,
    String message
) {}
