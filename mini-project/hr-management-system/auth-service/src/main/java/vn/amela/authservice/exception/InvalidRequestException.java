package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidRequestException extends AuthException {
    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
