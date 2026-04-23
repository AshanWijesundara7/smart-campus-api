package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Part 5 - API Request and Response Logging Filter
 *
 * Logs every incoming request (method + URI) and every outgoing
 * response (status code) using java.util.logging.Logger.
 *
 * Using a filter for cross-cutting concerns like logging is superior
 * to manually inserting Logger.info() in every method because:
 *  - Single place to change log format or level
 *  - New endpoints are automatically covered
 *  - Follows the DRY (Don't Repeat Yourself) principle
 *  - Can be toggled on/off globally without touching resource code
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Logs every incoming HTTP request: method + full URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
            "--> Incoming Request : [%s] %s",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Logs every outgoing HTTP response: status code + method + URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
            "<-- Outgoing Response: [%s] %s → HTTP %d",
            requestContext.getMethod(),
            requestContext.getUriInfo().getRequestUri(),
            responseContext.getStatus()
        ));
    }
}
