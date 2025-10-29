package org.ulinda.exceptions;

public class FrontendException extends RuntimeException {
    private final ErrorCode errorCode;
    private boolean showMessageToUser = false;

    public FrontendException(String message, boolean showMessageToUser) {
        this(message, null, null, showMessageToUser);
    }

    public FrontendException(String message, ErrorCode errorCode, boolean showMessageToUser) {
        this(message, errorCode, null, showMessageToUser);
    }

    public FrontendException(String message, Throwable cause, boolean showMessageToUser) {
        this(message, null, cause, showMessageToUser);
    }

    public FrontendException(String message, ErrorCode errorCode, Throwable cause, boolean showMessageToUser) {
        super(message, cause);
        this.errorCode = errorCode;
        this.showMessageToUser = showMessageToUser;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isShowMessageToUser() {
        return showMessageToUser;
    }
}
