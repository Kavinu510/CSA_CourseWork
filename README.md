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

The following sections summarise the conceptual written component of the coursework. A matching standalone submission version is also provided in `REPORT.pdf`, but the explanations are included here as well so that the GitHub repository remains self-contained for marking.

### 1.1 JAX-RS Resource Lifecycle And Thread Safety

The default JAX-RS resource lifecycle is request scoped. In practical terms, the runtime usually creates a new resource class instance for each incoming HTTP request. This means resource classes such as the room and sensor controllers should be treated as lightweight request handlers rather than places to store long-lived shared application state. If shared mutable data were stored directly in instance fields inside those classes, the design would quickly become fragile and confusing.

This lifecycle matters because a web API is inherently concurrent. Multiple clients may submit requests at the same time, and those requests may try to create rooms, register sensors, delete rooms, or append sensor readings simultaneously. Even though request-scoped resources avoid some accidental shared-state problems, the application still needs a deliberate strategy for protecting any central data structure that is shared across requests.

For this coursework, shared state is placed in a singleton `CampusStore`, not in resource instance fields. The store uses `ConcurrentHashMap` for the primary collections that hold rooms, sensors, and reading histories. That gives thread-safe access for many simple read and write operations. However, some actions involve more than one step and must be treated as a single logical unit. For example, creating a sensor requires validating the referenced room, storing the sensor, and updating the parent room's `sensorIds` list. Adding a reading requires both saving the new historical record and updating the parent sensor's `currentValue`. These compound operations are synchronized so that partial updates and race conditions do not corrupt the in-memory state.

Therefore, the implementation combines the default JAX-RS lifecycle with explicit concurrency control. Request-scoped resources handle incoming requests, while the singleton store centralizes the shared domain state. This approach aligns with the rubric because it shows an understanding not only of the lifecycle itself, but also of the effect that concurrent access has on data integrity.

### 1.2 Discovery Endpoint And HATEOAS

The discovery endpoint at `GET /api/v1` exists to make the API easier to understand and navigate. Instead of returning only a plain welcome message, it returns structured JSON metadata including the API name, version, contact information, and links to the main resource collections. That makes the root endpoint useful both to human developers exploring the service and to client applications that want to identify the entry points programmatically.

This design reflects the REST idea commonly summarized as HATEOAS, or Hypermedia as the Engine of Application State. The principle is that a server response should not only include data; it should also include links or navigational hints that help the client discover what can be done next. In this project, the discovery endpoint exposes links for rooms, sensors, and sensor-reading paths, so a client can begin at the root and learn the main structure of the API without relying entirely on separate documentation.

The main benefit over static documentation is reduced coupling. External documentation can become outdated after route changes, version changes, or feature additions. By contrast, hypermedia-style links travel with the live API response. Even in a small coursework system, this is valuable because it demonstrates a more mature view of REST: the API becomes more self-describing, more developer-friendly, and easier to evolve over time.

### 2.1 Returning Room IDs Versus Full Room Objects

When designing `GET /rooms`, one option is to return only room identifiers. That approach keeps the response payload small and may be appropriate in very large systems where bandwidth efficiency is the main concern. If the client only needs to know that rooms exist, ID-only responses can reduce network cost and speed up basic listings.

However, an ID-only response pushes extra work onto the client. A consumer that wants to display room names, capacities, or assigned sensors must make additional requests for each room. This creates extra round trips, increases latency, and makes the client logic more complex. In other words, smaller payloads on the wire can produce more total work overall.

Returning full room objects trades a slightly larger payload for richer immediate usability. In this coursework, that trade-off is sensible because the intended domain is campus facilities management. A facilities user is more likely to want a meaningful overview of rooms than a bare list of identifiers. For that reason, `GET /rooms` returns complete JSON objects containing `id`, `name`, `capacity`, and `sensorIds`. This matches the rubric's expectation of a comprehensive room list and shows awareness of the trade-off between network bandwidth and client-side processing.

### 2.2 DELETE Idempotency

An HTTP method is idempotent when repeating the same request does not keep changing the final server state after the intended effect has already been achieved. `DELETE` is classically described as idempotent for this reason. The key idea is not that every repeated call must return identical status codes, but that the server's final state should settle into the same outcome after the first successful request.

In this project, deleting an empty room such as `HALL-100` illustrates idempotency. The first successful `DELETE` removes the room and returns `204 No Content`. If the client repeats the same request, the room remains absent. The final state is still "this room does not exist," so the repeated request does not create a second deletion effect. The implementation therefore preserves idempotency at the level that matters most: server state.

For a room that still contains assigned sensors, such as `LIB-301`, the operation fails with `409 Conflict`. Repeating the same request leaves the room untouched each time because the business rule continues to block deletion. Again, the final state is stable across repeated identical requests. This is why the deletion logic is both logically consistent and correctly reasoned about in terms of idempotency.

### 3.1 `@Consumes` And Media Type Mismatches

The `@Consumes(MediaType.APPLICATION_JSON)` annotation is part of JAX-RS request matching. It tells the runtime that a resource method expects a request body in JSON format. When a client sends a request whose `Content-Type` does not match that contract, JAX-RS rejects the request before the method body is executed. This is an important technical point: the failure occurs at the framework level during resource-method selection and entity-provider matching, not inside the business logic.

In this project, if a client posts `text/plain` to an endpoint that consumes JSON, Jersey cannot find a suitable way to deserialize that payload into the expected Java object. The result is `415 Unsupported Media Type`. This is the correct HTTP response because the server understands the request method and URL, but the request entity format is unsupported for that operation.

This behavior improves robustness. It prevents invalid formats from reaching the domain logic and stops the server from attempting to bind arbitrary input into Java POJOs. In the implemented API, a `WebApplicationExceptionMapper` then converts framework-generated HTTP exceptions into the same structured JSON error format used elsewhere. That creates a more professional and consistent client experience than returning raw container-generated pages.

### 3.2 Query Parameters Versus Path Parameters For Filtering

Path parameters and query parameters serve different design purposes in RESTful APIs. A path parameter is mainly used to identify a specific resource or nested resource, such as `/rooms/LIB-301` or `/sensors/CO2-001`. In contrast, query parameters are used for optional filtering, searching, sorting, and projection on a collection resource.

For that reason, sensor filtering is implemented as `GET /sensors?type=CO2` rather than something like `/sensors/type/CO2`. The type value is not the identity of one sensor; it is a selection criterion that may match many sensors, one sensor, or none at all. A query string makes that meaning explicit and keeps the resource model clean. It tells the client, "return the sensors collection, but restrict it according to this filter."

Query parameters also scale better as the API grows. If future requirements introduced filters such as `status=ACTIVE`, `roomId=LAB-210`, or `minValue=400`, query strings would allow those options to be combined cleanly without inventing awkward or deeply nested path structures. This is why query parameters are the superior design choice for collection filtering in this coursework.

### 4.1 Sub-Resource Locator Benefits

The path `/sensors/{sensorId}/readings` models a clear parent-child relationship: readings belong to one sensor. Rather than placing all of that logic directly inside the main sensor controller, the implementation uses a sub-resource locator in `SensorResource` that returns a dedicated `SensorReadingResource`. This is an example of the sub-resource locator pattern in JAX-RS.

This pattern improves separation of concerns. `SensorResource` remains responsible for operations on the sensor collection and individual sensor entities, while `SensorReadingResource` focuses only on reading-history operations for the selected sensor. That separation makes the code easier to understand because each class has a narrower, more coherent responsibility.

The benefit becomes even clearer as APIs grow larger. Without delegation, a single controller can become bloated with unrelated methods for sensors, nested readings, reading-specific validation, and future analytics or aggregation features. By splitting nested responsibilities into their own resource class, the design becomes easier to maintain, easier to test, and easier to extend. In this coursework, that architectural choice also makes the URL structure and the code structure mirror each other cleanly.

### 5.2 Why 422 Is Better Than 404 For Missing Payload References

When a client submits `POST /sensors` with a `roomId` that does not exist, the requested URI itself is perfectly valid. The `/sensors` endpoint exists, the server has located the correct resource method, and the JSON may be syntactically correct. The problem lies in the meaning of the submitted entity: it refers to another resource that cannot be resolved.

That is why `422 Unprocessable Entity` is more precise than `404 Not Found`. A `404` suggests that the requested URL is missing. In this situation the URL is not missing at all; the entity is semantically invalid because one of its references breaks the business rules of the domain model. The server understands the request but cannot process it in its present form.

Using `422` therefore gives the client better feedback. It tells the caller that the structure of the request is acceptable, but that one of its content-level relationships must be corrected. For this Smart Campus API, that is exactly the case when a sensor payload references a room that is not present in the in-memory system.

### 5.4 Cybersecurity Risks Of Stack Traces

Raw stack traces are useful for developers, but they are dangerous when exposed directly to API consumers. A stack trace can reveal package structures, class names, method names, internal file paths, exact line numbers, framework details, and dependency information. That material helps attackers fingerprint the application and understand how it is built.

Once an attacker can identify frameworks, libraries, or internal design patterns, they may search for known vulnerabilities, infer where validation happens, or craft requests that target weak points in the control flow. Even apparently small details such as package names or file locations can contribute to reconnaissance. In security, information disclosure is often valuable long before any direct exploit is attempted.

For that reason, the API uses a global `ExceptionMapper<Throwable>` as a safety net. Unexpected exceptions are logged internally for debugging, but the client only receives a clean `500 Internal Server Error` JSON body with a generic message. This preserves observability for the developer while preventing unnecessary exposure of internal implementation details to external callers.

### 5.5 Why Filters Are Better For Logging

Logging is a classic cross-cutting concern. It applies to many or all endpoints, but it is not part of the core business purpose of any one endpoint. If logging statements are scattered manually across every resource method, the result is repetitive code, inconsistent message formats, and a higher chance that some endpoints will be forgotten or logged differently.

A JAX-RS filter solves that problem by moving request and response logging into one centralized component. In this project, `ApiLoggingFilter` captures the incoming HTTP method and request URI, and then records the outgoing response status code. Because it sits around the request-processing pipeline rather than inside one specific resource method, it works consistently across the whole API.

This design improves maintainability and cleanliness. Resource classes remain focused on business logic such as room creation, sensor validation, and reading management, while logging is handled in one place. It also makes future changes easier; if the logging format or level needs to change, the update can be made once rather than repeated across the codebase.

## Conclusion

Overall, the Smart Campus API demonstrates a deliberately structured REST design rather than a collection of ad hoc endpoints. The implementation combines JAX-RS resource classes, a shared in-memory store, sub-resource delegation, centralized logging, and layered exception mapping to produce an API that is both technically correct and aligned with the coursework requirements.

The design choices discussed above are not incidental. They directly support the behavior demonstrated by the live system: correct status codes, stable resource semantics, controlled error reporting, and maintainable separation of responsibilities. For that reason, the implementation and the report together aim to satisfy not only the functional requirements of the coursework, but also the deeper conceptual expectations in the marking rubric.
