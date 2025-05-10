package com.example.apisix.service;

import com.example.apisix.entity.*;
import com.example.apisix.repository.*;
import com.example.apisix.util.TemplateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.*;

@Service
@AllArgsConstructor
public class RouteService {
    private final ApiRepository apiRepo;
    private final TemplateRepository tplRepo;
    private final ApiBindingRepository apiBindingRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public void bindUserToApis(String userName, String personaType, String apiKey, List<String> apis, String url) {
        for (String apiName : apis) {
            ApiDefinition api = apiRepo.findByName(apiName);
            RouteTemplate tpl = tplRepo.findByCode(api.getRouteTemplateCode());

            if (api == null || tpl == null) {
                throw new RuntimeException("API or Template not found for: " + apiName);
            }

            // ==== Prepare template context ====
            Map<String, Object> context = new HashMap<>();
            context.put("userName", userName);
            context.put("personaType", personaType);
            context.put("apiKey", apiKey);
            context.put("uri", api.getUri());
            context.put("name", api.getName());
            context.put("serviceName", api.getServiceName());
            context.put("url", url);

            String upstreamId = api.getName() + "-upstream";
            context.put("upstream_id", upstreamId);

            // ==== Check and create upstream if needed ====
            String upstreamCheckUrl = "http://localhost:9180/apisix/admin/upstreams/" + upstreamId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", "admin123");

            boolean upstreamExists = true;
            try {
                restTemplate.exchange(upstreamCheckUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (HttpClientErrorException.NotFound e) {
                upstreamExists = false;
            } catch (Exception e) {
                throw new RuntimeException("Failed to check upstream existence", e);
            }

            if (!upstreamExists) {
                try {
                    URI parsed = new URI(url);
                    String hostPort = parsed.getHost() + ":" + (parsed.getPort() == -1 ? 80 : parsed.getPort());

                    Map<String, Object> upstream = Map.of(
                        "id", upstreamId,
                        "type", "roundrobin",
                        "nodes", Map.of(hostPort, 1)
                    );

                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> upstreamEntity = new HttpEntity<>(mapper.writeValueAsString(upstream), headers);
                    restTemplate.put(upstreamCheckUrl, upstreamEntity);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create upstream: " + upstreamId, e);
                }
            }

            // ==== Render vars_template ====
            String renderedVars = TemplateUtil.render(tpl.getVarsTemplate(), context);
            Map<String, List<List<String>>> varsMap;
            try {
                varsMap = mapper.readValue(renderedVars, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse vars_template JSON", e);
            }

            List<List<String>> vars = varsMap.get(personaType);
            if (vars == null) {
                throw new RuntimeException("No vars_template found for personaType: " + personaType);
            }

            // ==== Render plugin_template ====
            String pluginJson = TemplateUtil.render(tpl.getPluginTemplate(), context);
            Map<String, Object> plugins;
            try {
                plugins = mapper.readValue(pluginJson, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse plugin_template JSON", e);
            }

            // ==== Render route_template ====
            String routeJson = TemplateUtil.render(tpl.getRouteTemplate(), context);
            Map<String, Object> route;
            try {
                route = mapper.readValue(routeJson, Map.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse route_template JSON", e);
            }

            // ==== Assemble route ====
            String routeId = "route_" + api.getName() + "_" + userName;
            route.put("id", routeId);
            route.put("plugins", plugins);
            route.put("vars", vars);

            // ==== Push route to APISIX ====
            try {
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> routeEntity = new HttpEntity<>(mapper.writeValueAsString(route), headers);
                restTemplate.put("http://localhost:9180/apisix/admin/routes/" + routeId, routeEntity);
            } catch (Exception e) {
                throw new RuntimeException("Failed to push route to APISIX", e);
            }

            // ==== Record API binding ====
            ApiBinding record = new ApiBinding();
            record.setUserName(userName);
            record.setPersonaType(personaType);
            record.setApiName(api.getName());
            try {
                record.setBoundVars(mapper.writeValueAsString(vars));
                record.setTemplateContext(mapper.writeValueAsString(context));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize binding context", e);
            }

            apiBindingRepo.save(record);
        }
    }
}
