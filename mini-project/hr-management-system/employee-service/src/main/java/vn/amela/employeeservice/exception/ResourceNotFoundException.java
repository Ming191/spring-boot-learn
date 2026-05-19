package vn.amela.employeeservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends EmployeeException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
