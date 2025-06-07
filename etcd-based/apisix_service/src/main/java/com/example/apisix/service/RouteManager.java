package com.example.apisix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RouteManager {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public RouteManager(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public void createOrUpdateRoute(String routeId, Map<String, Object> route) {
        String url = "http://localhost:9180/apisix/admin/routes/" + routeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "admin123");
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(route), headers);
            restTemplate.put(url, entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize route", e);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
