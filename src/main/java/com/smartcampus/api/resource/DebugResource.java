package com.smartcampus.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {
    @GET
    @Path("/error")
    public String throwUnexpectedError() {
        throw new IllegalStateException("Intentional demo error for global exception mapping.");
    }
}
