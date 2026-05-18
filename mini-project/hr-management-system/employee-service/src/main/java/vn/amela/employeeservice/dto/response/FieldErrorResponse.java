package vn.amela.employeeservice.dto.response;

public record FieldErrorResponse(
        String field,
        String message
) {}