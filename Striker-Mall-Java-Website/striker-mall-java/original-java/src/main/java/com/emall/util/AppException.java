package com.emall.util;
/** Business-rule / data-access failure with a message safe to show to the user. */
public class AppException extends RuntimeException {
    public AppException(String m) { super(m); }
    public AppException(String m, Throwable c) { super(m, c); }
}
