package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class TokenRevokedException extends AuthException {
    public TokenRevokedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_REVOKED", message);
    }
}
