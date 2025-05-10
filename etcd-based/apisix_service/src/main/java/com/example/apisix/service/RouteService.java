package com.example.apisix.service;

import com.example.apisix.entity.*;
import com.example.apisix.repository.*;
import com.example.apisix.util.TemplateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@AllArgsConstructor
public class RouteService {
    private final ApiRepository apiRepo;
    private final TemplateRepository tplRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public void bindUserToApis(String userName, String personaType, String apiKey, List<String> apis) {
        for (String apiName : apis) {
            ApiDefinition api = apiRepo.findByName(apiName);
            RouteTemplate tpl = tplRepo.findByCode(api.getRouteTemplateCode());

            Map<String, Object> context = new HashMap<>();
            context.put("userName", userName);
            context.put("personaType", personaType);
            context.put("apiKey", apiKey);
            context.put("uri", api.getUri());
            context.put("name", api.getName());
            context.put("serviceName", api.getServiceName());

            // === 處理 vars_template（是 Map<String, List<List<String>>> 格式）===
            String rendered = TemplateUtil.render(tpl.getVarsTemplate(), context);
            Map<String, List<List<String>>> varsMap;
            try {
                varsMap = mapper.readValue(rendered, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse vars_template JSON", e);
            }

            List<List<String>> vars = varsMap.get(personaType);
            if (vars == null) {
                throw new RuntimeException("No vars_template found for personaType: " + personaType);
            }

            // === 渲染 Plugin Template ===
            String pluginJson = TemplateUtil.render(tpl.getPluginTemplate(), context);
            Map<String, Object> plugins;
            try {
                plugins = mapper.readValue(pluginJson, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse plugins", e);
            }

            // === 渲染 Route Template ===
            String routeJson = TemplateUtil.render(tpl.getRouteTemplate(), context);
            Map<String, Object> route;
            try {
                route = mapper.readValue(routeJson, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse route", e);
            }

            route.put("plugins", plugins);
            route.put("vars", vars);
            String id = "route_" + api.getName() + "_" + userName;
            route.put("id", id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", "admin123");
            HttpEntity<String> entity;
            try {
                entity = new HttpEntity<>(mapper.writeValueAsString(route), headers);
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert route to JSON", e);
            }

            restTemplate.put("http://localhost:9180/apisix/admin/routes/" + id, entity);
        }
    }
}
