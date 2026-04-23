package com.smartcampus.exception;

/**
 * Thrown when a POST reading is attempted on a sensor in MAINTENANCE status.
 * A sensor under maintenance is physically disconnected and cannot accept data.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {
    public SensorUnavailableException(String message) {
        super(message);
    }
}
