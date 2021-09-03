package com.epam.digital.data.platform.restapi.core.exception;

public class FileNotExclusiveException extends RuntimeException {
    public FileNotExclusiveException(String message) {
        super(message);
    }
}
