package vn.amela.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AuthException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    protected AuthException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
