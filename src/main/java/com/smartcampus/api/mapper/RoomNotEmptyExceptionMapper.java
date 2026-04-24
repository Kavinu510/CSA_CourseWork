package com.smartcampus.api.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.smartcampus.api.exception.RoomNotEmptyException;

@Provider
public class RoomNotEmptyExceptionMapper extends ErrorResponseFactory implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        return response(Response.Status.CONFLICT, exception.getMessage());
    }
}
