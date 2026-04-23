package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store for all campus data.
 *
 * Uses ConcurrentHashMap to safely handle concurrent requests.
 * JAX-RS creates a new resource instance per request, so all
 * resource classes must share this single DataStore instance.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>> sensorReadings = new ConcurrentHashMap<>();

    private DataStore() {
        seedData();
    }

    public static DataStore getInstance() {
        return INSTANCE;
    }

    /**
     * Pre-loads sample data so the API has something to demonstrate immediately.
     */
    private void seedData() {
        // Rooms
        Room r1 = new Room("LIB-301", "Library Quiet Study", 50);
        Room r2 = new Room("LAB-101", "Computer Lab", 30);
        Room r3 = new Room("HALL-A", "Main Lecture Hall", 200);
        rooms.put(r1.getId(), r1);
        rooms.put(r2.getId(), r2);
        rooms.put(r3.getId(), r3);

        // Sensors
        Sensor s1 = new Sensor("TEMP-001", "Temperature", "ACTIVE",      22.5,  "LIB-301");
        Sensor s2 = new Sensor("CO2-001",  "CO2",         "ACTIVE",      412.0, "LAB-101");
        Sensor s3 = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 0.0,   "HALL-A");

        sensors.put(s1.getId(), s1);
        sensors.put(s2.getId(), s2);
        sensors.put(s3.getId(), s3);

        // Link sensors to rooms
        r1.getSensorIds().add(s1.getId());
        r2.getSensorIds().add(s2.getId());
        r3.getSensorIds().add(s3.getId());

        // Initialise empty reading lists
        sensorReadings.put(s1.getId(), new ArrayList<>());
        sensorReadings.put(s2.getId(), new ArrayList<>());
        sensorReadings.put(s3.getId(), new ArrayList<>());
    }

    public Map<String, Room> getRooms() { return rooms; }
    public Map<String, Sensor> getSensors() { return sensors; }
    public Map<String, List<SensorReading>> getSensorReadings() { return sensorReadings; }
}
