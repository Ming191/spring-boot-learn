package vn.amela.leaveservice.exception;

public class ForbiddenActionException extends BusinessException {
    public ForbiddenActionException(String message) {
        super(message, ErrorCode.FORBIDDEN);
    }
}
