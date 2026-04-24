package com.smartcampus.api.mapper;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ThrowableExceptionMapper extends ErrorResponseFactory implements ExceptionMapper<Throwable> {
    private static final Logger LOGGER = Logger.getLogger(ThrowableExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.log(Level.SEVERE, "Unexpected API error", exception);
        return response(Response.Status.INTERNAL_SERVER_ERROR, "An unexpected server error occurred. Contact the API administrator.");
    }
}
