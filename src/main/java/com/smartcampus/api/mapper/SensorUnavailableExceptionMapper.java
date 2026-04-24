package com.smartcampus.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.smartcampus.api.exception.SensorUnavailableException;

@Provider
public class SensorUnavailableExceptionMapper extends ErrorResponseFactory implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException exception) {
        return response(Response.Status.FORBIDDEN, exception.getMessage());
    }
}
