package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 - Sensor Operations
 * Base path: /api/v1/sensors
 *
 * Handles sensor registration, retrieval, and filtered search.
 * Also acts as the parent locator for the sensor readings sub-resource.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     * GET /api/v1/sensors?type=CO2
     *
     * Returns all sensors, optionally filtered by type query parameter.
     */
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> result = store.getSensors().values().stream()
                .filter(s -> type == null || s.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     * Registers a new sensor. Validates that the referenced roomId exists.
     *
     * If a client sends Content-Type other than application/json,
     * JAX-RS automatically returns HTTP 415 Unsupported Media Type.
     */
    @POST
    public Response createSensor(Sensor sensor) {
        if (sensor.getId() == null || sensor.getId().isBlank()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Sensor 'id' field is required."))
                    .build();
        }
        if (sensor.getRoomId() == null || !store.getRooms().containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: room '" + sensor.getRoomId() + "' does not exist in the system."
            );
        }
        if (store.getSensors().containsKey(sensor.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }

        store.getSensors().put(sensor.getId(), sensor);
        store.getRooms().get(sensor.getRoomId()).getSensorIds().add(sensor.getId());
        store.getSensorReadings().put(sensor.getId(), new ArrayList<>());

        return Response.status(Response.Status.CREATED).entity(sensor).build();
    }

    /**
     * GET /api/v1/sensors/{sensorId}
     * Returns a single sensor by ID.
     */
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    /**
     * Sub-Resource Locator
     * /api/v1/sensors/{sensorId}/readings
     *
     * Delegates all reading-related paths to SensorReadingResource.
     * This pattern keeps each class focused on a single responsibility.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }
}
