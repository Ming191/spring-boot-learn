package vn.amela.employeeservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends EmployeeException {
    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
