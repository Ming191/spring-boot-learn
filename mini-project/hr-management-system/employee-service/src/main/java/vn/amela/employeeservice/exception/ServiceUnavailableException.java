package vn.amela.employeeservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends EmployeeException {
    public ServiceUnavailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }
}