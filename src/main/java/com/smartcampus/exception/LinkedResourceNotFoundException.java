package com.smartcampus.exception;

/**
 * Thrown when a sensor is registered with a roomId that does not exist.
 * The request URL is valid but the body references a missing resource.
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {
    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}
