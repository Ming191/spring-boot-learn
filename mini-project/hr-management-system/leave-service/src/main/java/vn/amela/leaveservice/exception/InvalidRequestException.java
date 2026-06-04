package vn.amela.leaveservice.exception;

public class InvalidRequestException extends BusinessException {
    public InvalidRequestException(String message) {
        super(message,  ErrorCode.INVALID_REQUEST);
    }
}
