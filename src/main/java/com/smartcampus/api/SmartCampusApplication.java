package com.smartcampus.api;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import com.smartcampus.api.filter.ApiLoggingFilter;
import com.smartcampus.api.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.api.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.api.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.api.mapper.ThrowableExceptionMapper;
import com.smartcampus.api.mapper.WebApplicationExceptionMapper;
import com.smartcampus.api.resource.DebugResource;
import com.smartcampus.api.resource.DiscoveryResource;
import com.smartcampus.api.resource.SensorResource;
import com.smartcampus.api.resource.SensorRoomResource;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {
    public SmartCampusApplication() {
        property(ServerProperties.WADL_FEATURE_DISABLE, true);
        register(JacksonFeature.class);
        register(DiscoveryResource.class);
        register(SensorRoomResource.class);
        register(SensorResource.class);
        register(DebugResource.class);
        register(ApiLoggingFilter.class);
        register(RoomNotEmptyExceptionMapper.class);
        register(LinkedResourceNotFoundExceptionMapper.class);
        register(SensorUnavailableExceptionMapper.class);   
        register(WebApplicationExceptionMapper.class);
        register(ThrowableExceptionMapper.class);
    }
}
