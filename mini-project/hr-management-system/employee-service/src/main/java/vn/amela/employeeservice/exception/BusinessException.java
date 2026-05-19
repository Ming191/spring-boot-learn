package vn.amela.employeeservice.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends EmployeeException {
    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }
}
