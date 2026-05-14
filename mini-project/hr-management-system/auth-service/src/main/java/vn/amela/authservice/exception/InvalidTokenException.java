package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", message);
    }
}
