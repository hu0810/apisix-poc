package com.example.apisix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class RouteManager {
    private static final String DEFAULT_ADMIN_URL = "http://localhost:9180/apisix/admin";
    private static final String DEFAULT_ADMIN_KEY = "admin123";

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String adminUrl;
    private final String adminKey;

    public RouteManager(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.adminUrl = System.getenv().getOrDefault("APISIX_ADMIN_URL", DEFAULT_ADMIN_URL);
        this.adminKey = System.getenv().getOrDefault("APISIX_ADMIN_API_KEY", DEFAULT_ADMIN_KEY);
    }

    public void createOrUpdateRoute(String routeId, Map<String, Object> route) {
        String url = adminUrl + "/routes/" + routeId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", adminKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(route), headers);
            restTemplate.put(url, entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize route", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call APISIX Admin API", e);
        }
    }
}
