package com.kisansetu.upload;

/** The uploaded file cannot be processed (wrong type, unreadable, missing columns...). Maps to HTTP 400. */
public class InvalidUploadException extends RuntimeException {

    public InvalidUploadException(String message) {
        super(message);
    }
}
