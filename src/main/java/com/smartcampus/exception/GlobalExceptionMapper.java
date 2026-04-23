package com.smartcampus.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Global safety net — catches ALL uncaught exceptions and returns HTTP 500.
 *
 * IMPORTANT: WebApplicationException (JAX-RS built-in exceptions like
 * NotFoundException 404, NotAllowedException 405, NotSupportedException 415)
 * are passed through unchanged. Without this check, JAX-RS 404s would be
 * incorrectly converted to 500 errors.
 *
 * This mapper ensures no raw Java stack traces are ever exposed to API
 * consumers, which could reveal internal class names, library versions,
 * file paths, and other information useful to attackers.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable e) {

        // Pass JAX-RS built-in exceptions through unchanged (404, 405, 415, etc.)
        if (e instanceof WebApplicationException) {
            return ((WebApplicationException) e).getResponse();
        }

        // Log the real error server-side only — never expose it to the client
        LOGGER.severe("Unexpected server error: [" + e.getClass().getSimpleName() + "] " + e.getMessage());

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status",  500,
                        "error",   "Internal Server Error",
                        "message", "An unexpected error occurred. Please contact support."
                ))
                .build();
    }
}
