package vn.amela.authservice.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AuthException {
    public TokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", message);
    }
}
