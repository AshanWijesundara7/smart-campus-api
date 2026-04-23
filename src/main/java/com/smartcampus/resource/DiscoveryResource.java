package com.smartcampus.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 - Discovery Endpoint
 * GET /api/v1/
 *
 * Returns API metadata including version, contact info, and
 * navigable links to all primary resource collections (HATEOAS).
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response discover() {

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms",   "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",         "Smart Campus API");
        response.put("version",     "v1");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors.");
        response.put("contact",     "admin@smartcampus.ac.uk");
        response.put("resources",   resources);

        return Response.ok(response).build();
    }
}
