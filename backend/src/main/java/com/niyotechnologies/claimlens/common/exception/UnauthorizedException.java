package com.niyotechnologies.claimlens.common.exception;

/**
 * Thrown for authentication failures (bad credentials, expired/invalid refresh token).
 * Deliberately does not distinguish "no such user" from "wrong password" at the message level.
 */
public class UnauthorizedException extends RuntimeException {

    private final String code;

    public UnauthorizedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
