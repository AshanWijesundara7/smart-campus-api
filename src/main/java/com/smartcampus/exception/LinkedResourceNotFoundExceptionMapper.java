package com.smartcampus.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Maps LinkedResourceNotFoundException to HTTP 422 Unprocessable Entity.
 *
 * 422 is more accurate than 404 here because the URL /sensors is valid.
 * The problem is inside the request body referencing a missing roomId,
 * which is a semantic/business-logic validation failure.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        return Response
                .status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of(
                        "status",  422,
                        "error",   "Unprocessable Entity",
                        "message", e.getMessage()
                ))
                .build();
    }
}
