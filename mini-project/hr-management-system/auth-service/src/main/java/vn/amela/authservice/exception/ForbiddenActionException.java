package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenActionException extends AuthException {
    public ForbiddenActionException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN_ACTION", message);
    }
}
