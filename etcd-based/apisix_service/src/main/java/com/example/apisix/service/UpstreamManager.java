package com.example.apisix.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

@Component
public class UpstreamManager {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public UpstreamManager(RestTemplate restTemplate, ObjectMapper mapper) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
    }

    public void ensureUpstream(Map<String, Object> desiredUpstream) {
        String upstreamId = (String) desiredUpstream.get("id");
        String url = "http://localhost:9180/apisix/admin/upstreams/" + upstreamId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "admin123");

        boolean needsCreate = true;
        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            Map<String, Object> existing = mapper.readValue(resp.getBody(), new TypeReference<>() {});
            Map<String, Object> node = (Map<String, Object>) existing.get("node");
            Map<String, Object> existingConf = node != null ? (Map<String, Object>) node.get("value") : null;

            if (existingConf != null &&
                Objects.equals(existingConf.get("type"), desiredUpstream.get("type")) &&
                Objects.equals(existingConf.get("nodes"), desiredUpstream.get("nodes"))) {
                needsCreate = false;
            }
        } catch (HttpClientErrorException.NotFound e) {
            needsCreate = true;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse existing upstream JSON", e);
        }

        if (needsCreate) {
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(desiredUpstream), headers);
                restTemplate.put(url, entity);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize upstream", e);
            }
        }
    }
}
