package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class InactiveUserException extends AuthException {
    public InactiveUserException() {
        super(HttpStatus.FORBIDDEN, "INACTIVE_USER", "User is inactive");
    }
}
