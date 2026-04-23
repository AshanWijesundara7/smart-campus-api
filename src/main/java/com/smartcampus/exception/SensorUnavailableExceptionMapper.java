package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps SensorUnavailableException to HTTP 403 Forbidden.
 *
 * 403 is appropriate because the resource exists but the operation
 * is not permitted given the sensor's current MAINTENANCE state.
 */
@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException e) {
        return Response
                .status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status",  403,
                        "error",   "Forbidden",
                        "message", e.getMessage()
                ))
                .build();
    }
}
