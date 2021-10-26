package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class JwtExpiredException extends RequestProcessingException {
    public JwtExpiredException(String message) {
        super(message, Status.JWT_EXPIRED);
    }
}
