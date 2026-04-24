package com.smartcampus.api.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {
    @GET
    public Response discover() {
        Map<String, Object> body = new LinkedHashMap<>();
        Map<String, String> contact = new LinkedHashMap<>();
        Map<String, String> resources = new LinkedHashMap<>();
        Map<String, String> sampleLinks = new LinkedHashMap<>();

        contact.put("name", "Smart Campus API Administrator");
        contact.put("email", "facilities-api@westminster.example");

        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        resources.put("sensorReadings", "/api/v1/sensors/{sensorId}/readings");

        sampleLinks.put("self", "/api/v1");
        sampleLinks.put("rooms", "/api/v1/rooms");
        sampleLinks.put("sensorsByType", "/api/v1/sensors?type=CO2");

        body.put("api", "Smart Campus Sensor and Room Management API");
        body.put("version", "1.0.0");
        body.put("basePath", "/api/v1");
        body.put("contact", contact);
        body.put("resources", resources);
        body.put("links", sampleLinks);

        return Response.ok(body).build();
    }
}
