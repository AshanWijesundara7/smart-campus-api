# Smart Campus API

A RESTful API built with JAX-RS (Jersey) for managing campus rooms and IoT sensors.

---
#Demo Video Link 
https://www.loom.com/share/21b5a3ebb61543788fdaab544630050e

## API Overview

This API manages three core entities:
- **Rooms** — Physical spaces on campus (e.g., labs, lecture halls)
- **Sensors** — IoT devices deployed inside rooms (temperature, CO2, occupancy)
- **Sensor Readings** — Historical measurement log per sensor

Base URL: `http://localhost:8080/api/v1`

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
    └── filter/
        └── LoggingFilter.java
```

---

## How to Build & Run

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

The server starts at: **http://localhost:8080/api/v1**

---

## Sample curl Commands

### 1. Discovery — GET /api/v1
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Get all rooms — GET /api/v1/rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 3. Create a new room — POST /api/v1/rooms
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"CS-201","name":"CS Lecture Hall","capacity":100}'
```

### 4. Get a specific room — GET /api/v1/rooms/{roomId}
```bash
curl -X GET http://localhost:8080/api/v1/rooms/LIB-301
```

### 5. Delete a room with sensors (returns 409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 6. Create a new sensor — POST /api/v1/sensors
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-002","type":"Temperature","status":"ACTIVE","currentValue":21.0,"roomId":"LAB-101"}'
```

### 7. Filter sensors by type — GET /api/v1/sensors?type=CO2
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 8. Post a sensor reading — POST /api/v1/sensors/{sensorId}/readings
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.3}'
```

### 9. Get sensor reading history — GET /api/v1/sensors/{sensorId}/readings
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### 10. Post to MAINTENANCE sensor (returns 403 Forbidden)
```bash
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":15}'
```

### 11. Create sensor with invalid roomId (returns 422 Unprocessable Entity)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-999","type":"CO2","status":"ACTIVE","currentValue":0,"roomId":"FAKE-999"}'
```

---

## Report: Answers to Questions

---

### Part 1 — Q1: JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of a resource class for every incoming HTTP request**. This is called the *per-request* lifecycle. Each request gets its own object, meaning instance variables are created fresh and then discarded after the response is sent.

This has a critical implication for in-memory data management: you **cannot store shared state** (such as a HashMap of rooms or sensors) as an instance field in a resource class, because it would be wiped out with every request. To solve this, all shared data must live in a **singleton** — in this project, the `DataStore` class uses a `private static final` instance so that all resource objects access the same data regardless of how many times the resource class is instantiated.

Additionally, because multiple requests can arrive simultaneously (concurrent threads), the `DataStore` uses `ConcurrentHashMap` instead of `HashMap`. A plain `HashMap` is not thread-safe and could cause data corruption or `ConcurrentModificationException` under concurrent writes. `ConcurrentHashMap` handles thread safety internally without requiring manual `synchronized` blocks.

---

### Part 1 — Q2: HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include **navigable links** to related resources and available actions, rather than just raw data. For example, a discovery response includes `"rooms": "/api/v1/rooms"` so clients can navigate to rooms without needing to know the URL structure in advance.

This benefits client developers in several ways. First, clients do not need to hardcode URLs — they follow links like a browser follows hyperlinks, making the client more resilient to URL changes. Second, the API becomes **self-documenting**: a new developer can start at the root endpoint and discover all available resources by following links. Third, as the API evolves, adding new resource links to responses notifies clients of new capabilities without breaking existing integrations. In contrast, static documentation becomes outdated and forces client developers to manually track API changes.

---

### Part 2 — Q1: Returning IDs vs Full Room Objects

Returning **only IDs** in a list response is bandwidth-efficient but forces the client to make one additional request per room to fetch its details — this is known as the **N+1 problem**. For a campus with thousands of rooms, this results in thousands of HTTP round-trips, adding significant latency and server load.

Returning **full room objects** increases the size of the initial response but gives the client everything it needs in a single request. This reduces total HTTP calls and improves client-side performance. The trade-off is payload size, which can be mitigated with pagination (e.g., returning 50 rooms per page). For most use cases in a campus management system, returning full objects with pagination is the preferred approach as it reduces latency and simplifies client logic.

---

### Part 2 — Q2: Is DELETE Idempotent?

Yes, `DELETE` is idempotent in this implementation. Idempotency means that making the same request multiple times produces the same server state as making it once.

- **First call**: The room exists → it is deleted → server returns `204 No Content`.
- **Second call**: The room no longer exists → server returns `404 Not Found`.

The HTTP status codes differ between calls, but the **server state is identical** after both: the room is absent. This satisfies the REST definition of idempotency. The client should not be concerned that a `404` on a second `DELETE` means something went wrong — it simply confirms the resource was already removed.

---

### Part 3 — Q1: @Consumes Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that this endpoint only accepts JSON-formatted request bodies. If a client sends a request with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS **automatically rejects the request** before the method is ever called, returning an **HTTP 415 Unsupported Media Type** response. The developer does not need to write any validation code for this — the framework enforces it at the routing level. This protects the API from receiving data in unexpected formats that could cause parsing errors or security issues.

---

### Part 3 — Q2: @QueryParam vs Path Segment for Filtering

Using `@QueryParam` for filtering (`GET /sensors?type=CO2`) is semantically correct because the **resource being requested is still the sensors collection** — the query parameter simply narrows it down. The base URL `/sensors` represents the full collection, and the filter is an optional refinement.

Using a path segment (`/sensors/type/CO2`) is problematic because it implies that `type` and `CO2` are themselves resources in a hierarchy, which they are not. It also becomes unwieldy when combining multiple filters — for example, `/sensors/type/CO2/status/ACTIVE` is verbose and hard to read, whereas `?type=CO2&status=ACTIVE` is concise and follows the standard query string convention. Furthermore, query parameters are treated as optional by nature, making it natural to return all sensors when no filter is supplied, which aligns with REST principles for collection resources.

---

### Part 4 — Q1: Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates path handling to a separate, dedicated class rather than defining all nested endpoints in one large resource class. For example, instead of handling `/sensors/{id}/readings` and `/sensors/{id}/readings/{rid}` inside `SensorResource`, the locator method returns a `SensorReadingResource` instance that handles those paths independently.

The architectural benefit is **separation of concerns**: each class is responsible for one resource type and one level of the hierarchy. In a large API with dozens of nested paths, a single "god class" handling everything becomes difficult to read, test, and maintain. Separate classes follow the **Single Responsibility Principle**, making each file shorter, easier to unit test in isolation, and easier for teams to work on in parallel. Adding new sub-resources (e.g., `/sensors/{id}/alerts`) simply means creating a new class and adding one locator method, without touching existing code.

---

### Part 5 — Q2: HTTP 422 vs 404

A **404 Not Found** means the **URL itself** refers to a resource that does not exist. For example, requesting `/rooms/FAKE-999` returns 404 because that room URL does not exist.

A **422 Unprocessable Entity** means the request URL is valid and the JSON body is syntactically correct, but the **semantic content is invalid** — in this case, the `roomId` field inside the JSON body points to a room that does not exist in the system.

When a client sends `POST /sensors` with `"roomId": "FAKE-999"`, the URL `/sensors` is perfectly valid (it exists). The problem is entirely within the payload. Using 404 would mislead the client into thinking the `/sensors` endpoint doesn't exist, which is incorrect. HTTP 422 precisely communicates: "I understood your request, but the data inside it refers to something that doesn't exist", which is a business-logic validation failure rather than a missing URL.

---

### Part 5 — Q4: Security Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers creates several security vulnerabilities:

1. **Internal architecture disclosure**: Class names and package names (e.g., `com.smartcampus.store.DataStore`) reveal the internal structure of the application, helping attackers map out the codebase.
2. **Library and framework versions**: Stack traces often include third-party library class names and version-specific call paths (e.g., Jersey, Jackson). Attackers can cross-reference these against known CVEs (Common Vulnerabilities and Exposures) to identify exploitable vulnerabilities.
3. **File system paths**: Stack traces can include absolute file paths on the server (e.g., `/home/ubuntu/app/...`), revealing the deployment environment and directory structure.
4. **Database and query information**: In database-related errors, stack traces may reveal table names, column names, or SQL query fragments, enabling SQL injection attempts.
5. **Business logic leakage**: The sequence of method calls in a trace reveals how the application processes data internally, which can be exploited to craft targeted malicious inputs.

The `GlobalExceptionMapper` prevents all of this by catching every unhandled exception and returning only a generic "Internal Server Error" message, logging the actual details safely on the server side only.

---

### Part 5 — Q5: Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging is superior to manually inserting `Logger.info()` statements inside every resource method for several reasons:

1. **Single point of control**: The log format, log level, or logging library can be changed in one file rather than across dozens of methods.
2. **Guaranteed coverage**: A filter automatically applies to every request and response, including new endpoints added in the future. Manual logging can be accidentally omitted when adding new methods.
3. **DRY principle**: Repeating the same logging boilerplate in every method violates Don't Repeat Yourself, making the codebase larger and harder to maintain.
4. **Separation of concerns**: Resource methods should handle business logic, not infrastructure concerns like logging. Filters keep these responsibilities cleanly separated.
5. **Easier to enable/disable**: A filter can be toggled on or off globally (e.g., via configuration), while manual logging requires editing every method.
