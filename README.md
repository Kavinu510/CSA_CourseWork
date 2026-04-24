# Smart Campus Sensor and Room Management API

JAX-RS coursework implementation for the Client-Server Architectures module. The service manages smart campus rooms, sensors, and historical sensor readings through a versioned REST API.

This project intentionally uses JAX-RS with Jersey and an in-memory Java collection store only. It does not use Spring Boot or any database technology.

## Technology Stack

- Java 17 source level
- Maven
- JAX-RS using Jersey 2.x
- Apache Tomcat 9 servlet container
- Maven WAR packaging
- Jackson JSON provider for Jersey

## Build And Run

```powershell
.\mvnw.cmd clean package
```

Deploy the generated WAR file to Apache Tomcat 9 from NetBeans. In NetBeans, right-click the project, open `Properties > Run`, choose Apache Tomcat 9, and set the context path to:

```text
/smart-campus-api
```

After deployment, the API root is:

```text
http://localhost:8080/smart-campus-api/api/v1
```

If Maven is globally installed, the equivalent package command is `mvn clean package`.

## API Endpoints

| Method | Path                                  | Purpose                                              |
| ------ | ------------------------------------- | ---------------------------------------------------- |
| GET    | `/api/v1`                             | Discovery metadata, contact info, and resource links |
| GET    | `/api/v1/rooms`                       | List all rooms as full JSON objects                  |
| POST   | `/api/v1/rooms`                       | Create a room                                        |
| GET    | `/api/v1/rooms/{roomId}`              | Fetch one room                                       |
| DELETE | `/api/v1/rooms/{roomId}`              | Delete a room if no sensors are assigned             |
| GET    | `/api/v1/sensors`                     | List sensors, optionally filtered by `type`          |
| POST   | `/api/v1/sensors`                     | Register a sensor after validating `roomId`          |
| GET    | `/api/v1/sensors/{sensorId}`          | Fetch one sensor                                     |
| GET    | `/api/v1/sensors/{sensorId}/readings` | Fetch sensor reading history                         |
| POST   | `/api/v1/sensors/{sensorId}/readings` | Add a reading and update parent sensor value         |
| GET    | `/api/v1/debug/error`                 | Demonstrate global 500 error mapping                 |

## Sample Curl Commands

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1
```

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1/rooms
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"ART-101","name":"Design Studio","capacity":18}'
```

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1/rooms/ART-101
```

```bash
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/HALL-100
```

```bash
curl -i -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/LIB-301
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"LIGHT-010","type":"Lighting","status":"ACTIVE","currentValue":1,"roomId":"ART-101"}'
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"CO2","status":"ACTIVE","currentValue":410,"roomId":"NO-ROOM"}'
```

```bash
curl -i "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":421.8}'
```

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-001
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/OCC-009/readings \
  -H "Content-Type: application/json" \
  -d '{"value":8}'
```

```bash
curl -i -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: text/plain" \
  -d "plain text"
```

```bash
curl -i http://localhost:8080/smart-campus-api/api/v1/debug/error
```

## Seed Data

The API starts with demo data so the Postman video can immediately show success and error paths.

| Resource           | IDs                              |
| ------------------ | -------------------------------- |
| Rooms              | `LIB-301`, `LAB-210`, `HALL-100` |
| Active sensors     | `TEMP-001`, `CO2-001`            |
| Maintenance sensor | `OCC-009`                        |

`HALL-100` has no sensors and can be deleted successfully. `LIB-301` has assigned sensors and returns `409 Conflict` if deletion is attempted.

## Conceptual Report Answers

### 1.1 JAX-RS Resource Lifecycle And Thread Safety

The default JAX-RS resource lifecycle is request scoped. The runtime normally creates a new resource class instance for each incoming HTTP request. That means instance fields inside resource classes should not be treated as long-lived shared storage, because a different resource instance may handle the next request.

For this coursework, shared state is placed in a singleton `CampusStore`, not in resource instance fields. The store uses `ConcurrentHashMap` for the main collections and synchronized methods for compound operations that must be atomic, such as registering a sensor and adding its ID to the parent room, deleting a room only after checking its sensors, and adding a reading while updating the parent sensor's `currentValue`. This avoids lost updates and race conditions while still keeping the implementation in memory as required.

### 1.2 Discovery Endpoint And HATEOAS

Hypermedia is a hallmark of advanced RESTful design because responses do not only return data; they also show clients where related actions and resources are located. The discovery endpoint returns version information, contact details, and links to the main collections. This makes the API more self-documenting and helps client developers navigate from the API root to rooms, sensors, and readings without hard-coding every URL from separate documentation.

Static documentation can become stale when routes change. Hypermedia links travel with the API response, so clients can discover supported navigation paths at runtime and reduce coupling to fixed URL assumptions.

### 2.1 Returning Room IDs Versus Full Room Objects

Returning only room IDs keeps payloads smaller and saves bandwidth, especially when there are thousands of rooms or mobile clients with limited network capacity. However, clients then need to make extra requests to fetch each room's name, capacity, and sensor list, which increases latency and client-side processing.

Returning full room objects costs more bandwidth but gives clients immediately useful data in one response. For this coursework, `GET /rooms` returns full room objects because facilities managers need a comprehensive overview and the rubric asks for a comprehensive room list.

### 2.2 DELETE Idempotency

The successful room deletion operation is idempotent in server-state terms. If a client deletes an empty room such as `HALL-100`, the first request removes it and returns `204 No Content`. If the same request is sent again, the room is still absent, and the server state remains unchanged. The response can still be `204 No Content` because the desired state, "this room no longer exists", has already been achieved.

For a room with assigned sensors, repeated DELETE requests return `409 Conflict` and leave the room unchanged each time. That also keeps the server state stable across repeated identical requests.

### 3.1 `@Consumes` And Media Type Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the method accepts JSON request bodies. If a client sends `text/plain` or `application/xml` to that method, Jersey cannot match the request entity media type to the resource method. The runtime rejects the request before the method logic executes and returns `415 Unsupported Media Type`.

This protects the API from trying to deserialize unsupported formats as Java POJOs. In this project, the `WebApplicationExceptionMapper` converts that failure into a structured JSON error response instead of a default server page.

### 3.2 Query Parameters Versus Path Parameters For Filtering

Path parameters are best for identifying a specific resource or nested resource, such as `/sensors/CO2-001`. Query parameters are better for filtering, searching, sorting, and optional modifiers on a collection, such as `/sensors?type=CO2`.

The query parameter approach is superior here because `type` is not a unique resource identity. It is a search criterion that may match many sensors or no sensors. Query parameters also scale naturally if more filters are added later, such as `status=ACTIVE` or `roomId=LIB-301`, without creating awkward nested paths for every possible search combination.

### 4.1 Sub-Resource Locator Benefits

The sub-resource locator in `SensorResource` delegates `/sensors/{sensorId}/readings` to a dedicated `SensorReadingResource`. This keeps the main sensor controller focused on sensor collection and sensor detail operations, while reading history logic is isolated in a class that understands the selected parent sensor context.

In larger APIs, this pattern prevents one massive controller from containing every nested path such as sensors, readings, reading IDs, and future reading analytics. It improves maintainability, testing, readability, and separation of responsibilities.

### 5.2 Why 422 Is Better Than 404 For Missing Payload References

When a client posts a sensor with a non-existent `roomId`, the endpoint `/sensors` exists and the JSON body may be syntactically valid. The problem is semantic: the payload references a room that cannot be linked. `422 Unprocessable Entity` describes that situation more accurately than `404 Not Found`.

A `404` would suggest the requested URL was missing. Here the URL is valid, but the server cannot process the entity because one of its internal references violates business rules.

### 5.4 Cybersecurity Risks Of Stack Traces

Raw Java stack traces expose implementation details that attackers can use. They may reveal package names, class names, method names, line numbers, server file paths, framework versions, dependency names, and internal control flow. That information helps attackers fingerprint the technology stack and search for known vulnerabilities or weak points.

The global exception mapper returns a generic `500 Internal Server Error` JSON body to external clients while logging the real exception internally for developers. This keeps the API leak-proof without losing observability.

### 5.5 Why Filters Are Better For Logging

Logging is a cross-cutting concern because it applies to every endpoint rather than one business operation. A JAX-RS request/response filter centralizes that concern in one class, so every incoming request and outgoing status code is logged consistently.

If `Logger.info()` calls were manually placed in every resource method, the code would become repetitive, easy to forget, and harder to maintain. Filters keep resource methods focused on business logic while still providing API observability.
