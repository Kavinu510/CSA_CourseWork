package com.smartcampus.api.mapper;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.smartcampus.api.model.ErrorResponse;

abstract class ErrorResponseFactory {
    @Context
    private UriInfo uriInfo;

    protected Response response(Response.Status status, String message) {
        return response(status.getStatusCode(), status.getReasonPhrase(), message);
    }

    protected Response response(int statusCode, String reason, String message) {
        String path = uriInfo == null ? "" : uriInfo.getPath();
        ErrorResponse error = new ErrorResponse(statusCode, reason, message, "/api/v1/" + path);
        return Response.status(statusCode).entity(error).build();
    }
}
