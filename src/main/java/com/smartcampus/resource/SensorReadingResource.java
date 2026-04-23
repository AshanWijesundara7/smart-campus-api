package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4 - Sub-Resource for Sensor Readings
 * Reached via: /api/v1/sensors/{sensorId}/readings
 *
 * This class is NOT annotated with @Path — it is instantiated by the
 * sub-resource locator in SensorResource.
 *
 * Side Effect: A successful POST updates the parent Sensor's currentValue
 * to maintain data consistency across the API.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * GET /api/v1/sensors/{sensorId}/readings
     * Returns the full reading history for the specified sensor.
     */
    @GET
    public Response getReadings() {
        if (!store.getSensors().containsKey(sensorId)) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }
        List<SensorReading> readings = store.getSensorReadings()
                .getOrDefault(sensorId, new ArrayList<>());
        return Response.ok(readings).build();
    }

    /**
     * POST /api/v1/sensors/{sensorId}/readings
     * Appends a new reading to this sensor's history.
     *
     * Business rules:
     *  - Sensor must exist
     *  - Sensor must NOT be in MAINTENANCE status (throws 403)
     *  - UUID and timestamp are auto-generated
     *  - Parent Sensor's currentValue is updated with the new reading
     */
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensors().get(sensorId);

        if (sensor == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Sensor not found: " + sensorId))
                    .build();
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                "and cannot accept new readings."
            );
        }

        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        store.getSensorReadings()
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // Side effect: keep parent sensor currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}
