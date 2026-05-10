package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends AuthException {
    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
}
