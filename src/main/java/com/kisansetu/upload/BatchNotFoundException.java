package com.kisansetu.upload;

public class BatchNotFoundException extends RuntimeException {

    public BatchNotFoundException(Long id) {
        super("Batch " + id + " not found");
    }
}
