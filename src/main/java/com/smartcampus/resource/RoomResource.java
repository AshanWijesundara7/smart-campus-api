package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collection;
import java.util.Map;

/**
 * Part 2 - Room Management
 * Base path: /api/v1/rooms
 *
 * Handles all CRUD operations for campus rooms.
 * Business rule: A room cannot be deleted if it still has sensors assigned.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/rooms
     * Returns all rooms in the system.
     */
    @GET
    public Response getAllRooms() {
        Collection<Room> rooms = store.getRooms().values();
        return Response.ok(rooms).build();
    }

    /**
     * POST /api/v1/rooms
     * Creates a new room. Returns 201 Created on success.
     */
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isBlank()) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Room 'id' field is required."))
                    .build();
        }
        if (store.getRooms().containsKey(room.getId())) {
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(Map.of("error", "A room with ID '" + room.getId() + "' already exists."))
                    .build();
        }
        store.getRooms().put(room.getId(), room);
        return Response.status(Response.Status.CREATED).entity(room).build();
    }

    /**
     * GET /api/v1/rooms/{roomId}
     * Returns a single room by ID. Returns 404 if not found.
     */
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    /**
     * DELETE /api/v1/rooms/{roomId}
     * Deletes a room. Blocked with 409 if the room still has sensors.
     *
     * Idempotency: First call → 204 No Content (deleted).
     * Subsequent calls → 404 Not Found (already gone).
     * Server state (room absent) is the same after both — DELETE is idempotent.
     */
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRooms().get(roomId);
        if (room == null) {
            return Response
                    .status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Room not found: " + roomId))
                    .build();
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + roomId + "'. It still has " +
                room.getSensorIds().size() + " sensor(s) assigned. Remove all sensors first."
            );
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }
}
