package vn.amela.leaveservice.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) { }
