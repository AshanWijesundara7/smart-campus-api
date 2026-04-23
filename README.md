# Smart Campus API

A RESTful API built with JAX-RS (Jersey) and an embedded Grizzly server for managing campus rooms and IoT sensors.

---

## API Overview

| Resource | Base Path |
|---|---|
| Discovery | `GET /api/v1/` |
| Rooms | `/api/v1/rooms` |
| Sensors | `/api/v1/sensors` |
| Readings | `/api/v1/sensors/{sensorId}/readings` |

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java
    ├── SmartCampusApplication.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   └── SensorReading.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── RoomNotEmptyExceptionMapper.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── LinkedResourceNotFoundExceptionMapper.java
    │   ├── SensorUnavailableException.java
    │   ├── SensorUnavailableExceptionMapper.java
    │   └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java
```

---

## How to Build and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smart-campus-api.git
cd smart-campus-api

# 2. Build the fat JAR
mvn clean package

# 3. Run the server
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

Server starts at: **http://localhost:8080/api/v1**

---

## Sample curl Commands

### 1. Discovery
```bash
curl -X GET http://localhost:8080/api/v1/
```

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CS-201","name":"CS Lecture Hall","capacity":100}'
```

### 4. Get a specific room
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Delete a room (blocked — has sensors → 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 6. Delete an empty room (success → 204 No Content)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/CS-201
```

### 7. Create a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LAB-101"}'
```

### 8. Create sensor with invalid roomId (→ 422 Unprocessable Entity)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-999","type":"CO2","status":"ACTIVE","currentValue":0,"roomId":"FAKE-999"}'
```

### 9. Filter sensors by type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 10. Post a reading to an active sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'
```

### 11. Post a reading to a MAINTENANCE sensor (→ 403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15}'
```

### 12. Get reading history for a sensor
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

---

## Report: Answers to Questions

### Part 1 — Q1: JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request** (per-request lifecycle). Each request gets its own object, so instance variables are created fresh and discarded after the response is sent.

This means shared state such as a HashMap of rooms or sensors cannot be stored as an instance field in a resource class — it would reset with every request. The solution used in this project is a **singleton DataStore** using a `private static final` instance, ensuring all resource objects access the same data regardless of how many times they are instantiated.

Additionally, because multiple requests can arrive simultaneously, the DataStore uses `ConcurrentHashMap` instead of `HashMap`. A plain `HashMap` is not thread-safe and can cause data corruption or `ConcurrentModificationException` under concurrent writes. `ConcurrentHashMap` handles thread safety internally.

---

### Part 1 — Q2: HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means API responses include navigable links to related resources and available actions, rather than just raw data. For example, the discovery response includes `"rooms": "/api/v1/rooms"` so clients can navigate without knowing the URL structure in advance.

Benefits over static documentation: clients do not need to hardcode URLs and can follow links like a browser, making them resilient to URL changes. The API becomes self-documenting — a new developer can start at the root and discover all resources by following links. As the API evolves, adding new links to responses notifies clients of new capabilities without breaking existing integrations.

---

### Part 2 — Q1: Returning IDs vs Full Room Objects

Returning only IDs is bandwidth-efficient but forces the client to make one additional HTTP request per room to fetch details — the N+1 problem. For a campus with thousands of rooms, this means thousands of round-trips, adding significant latency and server load.

Returning full room objects increases the initial response size but gives the client everything in a single request, reducing total HTTP calls. The trade-off of payload size can be managed with pagination. For most campus management use cases, returning full objects with pagination is preferred as it reduces latency and simplifies client logic.

---

### Part 2 — Q2: Is DELETE Idempotent?

Yes, DELETE is idempotent in this implementation. The first call removes the room and returns `204 No Content`. Any subsequent identical DELETE returns `404 Not Found`. The server state — the room being absent — is identical after both calls. This satisfies the REST definition of idempotency: the outcome of making the same request multiple times is the same as making it once.

---

### Part 3 — Q1: @Consumes Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS this endpoint only accepts JSON. If a client sends `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS automatically rejects the request before the method is ever invoked, returning **HTTP 415 Unsupported Media Type**. No custom validation code is needed — the framework enforces it at the routing level.

---

### Part 3 — Q2: @QueryParam vs Path Segment for Filtering

Using `@QueryParam` for filtering (`GET /sensors?type=CO2`) is semantically correct because the resource being requested is still the sensors collection — the parameter simply narrows it. The base URL `/sensors` represents the full collection and the filter is an optional refinement.

A path segment (`/sensors/type/CO2`) implies that `type` and `CO2` are resources in a hierarchy, which they are not. It also becomes unwieldy with multiple filters — `/sensors/type/CO2/status/ACTIVE` is verbose compared to `?type=CO2&status=ACTIVE`. Query parameters are treated as optional by nature, making it natural to return all sensors when no filter is provided.

---

### Part 4 — Q1: Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates path handling to a dedicated class rather than defining all nested endpoints in one large controller. Instead of handling `/sensors/{id}/readings` inside `SensorResource`, a locator method returns a `SensorReadingResource` instance that handles those paths independently.

The benefit is separation of concerns: each class is responsible for one resource type. In a large API, a single class handling all nested paths becomes a "god class" — difficult to read, test, and maintain. Separate classes follow the Single Responsibility Principle, making each file shorter, independently testable, and easier for teams to work on in parallel.

---

### Part 5 — Q2: HTTP 422 vs 404

A `404 Not Found` means the requested URL does not exist. A `422 Unprocessable Entity` means the URL is valid and the JSON is syntactically correct, but the semantic content is invalid — the `roomId` field references a room that does not exist.

When a client sends `POST /sensors` with `"roomId": "FAKE-999"`, the URL `/sensors` is valid. Using 404 would mislead the client into thinking the endpoint does not exist. HTTP 422 precisely communicates a business-logic validation failure inside the payload, which is more accurate and more useful to the client.

---

### Part 5 — Q4: Security Risks of Exposing Stack Traces

Exposing raw Java stack traces creates several vulnerabilities. Internal class and package names reveal application architecture, helping attackers map the codebase. Library and framework names with version-specific call paths allow attackers to cross-reference known CVEs. Server file paths expose the deployment environment. Database-related errors may reveal table names or SQL fragments, enabling injection attacks. The sequence of method calls reveals how the application processes data, which can be exploited to craft targeted malicious inputs.

The `GlobalExceptionMapper` prevents all of this by catching every unhandled exception and returning only a generic message, while logging full details safely on the server only.

---

### Part 5 — Q5: Filters vs Manual Logging

Using JAX-RS filters for logging is superior to manually inserting `Logger.info()` in every method because: the log format can be changed in one place rather than across dozens of methods; new endpoints are automatically covered without any extra work; the DRY principle is maintained; resource methods stay focused on business logic rather than infrastructure concerns; and the filter can be toggled globally without editing any resource code.
