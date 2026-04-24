package com.smartcampus.api.resource;

import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.smartcampus.api.model.ReadingRequest;
import com.smartcampus.api.model.SensorReading;
import com.smartcampus.api.store.CampusStore;

@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {
    private final CampusStore store = CampusStore.getInstance();
    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadings() {
        List<SensorReading> readings = store.getReadings(sensorId);
        if (readings == null) {
            throw new NotFoundException("Sensor " + sensorId + " was not found.");
        }
        return readings;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(ReadingRequest request, @Context UriInfo uriInfo) {
        SensorReading created = store.addReading(sensorId, request.getValue());
        if (created == null) {
            throw new NotFoundException("Sensor " + sensorId + " was not found.");
        }
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId()).build();
        return Response.created(location).entity(created).build();
    }
}
