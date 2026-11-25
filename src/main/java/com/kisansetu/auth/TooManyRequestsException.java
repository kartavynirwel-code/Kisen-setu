package com.kisansetu.auth;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("Too many failed attempts. Try again later");
    }
}
