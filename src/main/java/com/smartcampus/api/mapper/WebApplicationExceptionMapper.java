package com.smartcampus.api.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper extends ErrorResponseFactory implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException exception) {
        Response original = exception.getResponse();
        int status = original == null ? Response.Status.BAD_REQUEST.getStatusCode() : original.getStatus();
        Response.Status statusInfo = Response.Status.fromStatusCode(status);
        String reason = statusInfo == null ? "HTTP Error" : statusInfo.getReasonPhrase();
        String message = exception.getMessage() == null || exception.getMessage().isBlank()
                ? reason
                : exception.getMessage();
        return response(status, reason, message);
    }
}
