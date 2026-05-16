package vn.amela.employeeservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class EmployeeException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public EmployeeException(String message,
                             HttpStatus status,
                             String code) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
