package com.kisansetu.claim;

/** The request is well-formed but not acceptable (e.g. rejecting a claim without a reason). Maps to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
