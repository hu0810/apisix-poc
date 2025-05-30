package com.example.apisix.service;

import com.example.apisix.entity.*;
import com.example.apisix.repository.*;
import com.example.apisix.utils.TemplateUtil;
import com.example.apisix.utils.TemplateValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final ApiRepository apiRepo;
    private final TemplateRepository tplRepo;
    private final UpstreamTemplateRepository upstreamTplRepo;
    private final ApiBindingRepository apiBindingRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public void bindSmart(String userName, String personaType, String apiKey, List<String> apis, Map<String, Object> extraParams) {
        for (String apiName : apis) {
            ApiDefinition api = apiRepo.findByName(apiName);
            RouteTemplate tpl = tplRepo.findByCode(api.getRouteTemplateCode());
            UpstreamTemplate upstreamTpl = upstreamTplRepo.findByCode(api.getUpstreamTemplateCode());

            if (api == null || tpl == null || upstreamTpl == null) {
                throw new RuntimeException("API or Template not found for: " + apiName);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("userName", userName);
            context.put("personaType", personaType);
            context.put("apiKey", apiKey);
            context.put("uri", api.getUri());
            context.put("name", api.getName());
            context.put("serviceName", api.getServiceName());
            if (extraParams != null) {
                context.putAll(extraParams);
                Object rawNodes = extraParams.get("nodes");
                if (rawNodes instanceof List && !((List<?>) rawNodes).isEmpty()) {
                    context.put("nodes", rawNodes);
                }
            }

            Map<String, String> allTemplates = Map.of(
                "route_template", tpl.getRouteTemplate(),
                "plugin_template", tpl.getPluginTemplate(),
                "vars_template", tpl.getVarsTemplate(),
                "upstream_template", upstreamTpl.getUpstreamTemplate()
            );
            TemplateValidator.validateAllTemplates(allTemplates, context);
            TemplateValidator.validateIfNodeTemplateUsed(upstreamTpl.getUpstreamTemplate(), context);

            String renderedUpstreamStr = TemplateUtil.render(upstreamTpl.getUpstreamTemplate(), context);
            Map<String, Object> desiredUpstream;
            try {
                desiredUpstream = mapper.readValue(renderedUpstreamStr, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse rendered upstream_template JSON", e);
            }

            String upstreamIdFromTpl = (String) desiredUpstream.get("id");
            if (upstreamIdFromTpl == null) {
                throw new RuntimeException("Missing upstream id in rendered template.");
            }

            String upstreamCheckUrl = "http://localhost:9180/apisix/admin/upstreams/" + upstreamIdFromTpl;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", "admin123");

            boolean needsCreate = true;
            try {
                ResponseEntity<String> resp = restTemplate.exchange(upstreamCheckUrl, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                Map<String, Object> existing = mapper.readValue(resp.getBody(), new TypeReference<>() {});
                Map<String, Object> existingValue = (Map<String, Object>) existing.get("node");
                Map<String, Object> existingConf = existingValue != null ? (Map<String, Object>) existingValue.get("value") : null;

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
                    HttpEntity<String> upstreamEntity = new HttpEntity<>(mapper.writeValueAsString(desiredUpstream), headers);
                    restTemplate.put(upstreamCheckUrl, upstreamEntity);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize upstream", e);
                }
            }

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

            // Java side 檢查 plugin 渲染結果中是否含警告

            Map<String, Object> plugins;
            try {
                String pluginJson = TemplateUtil.render(tpl.getPluginTemplate(), context);
                plugins = mapper.readValue(pluginJson, new TypeReference<>() {});

                if (plugins.containsKey("__template_warning__")) {
                    throw new IllegalArgumentException((String) plugins.get("__template_warning__"));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse plugin_template JSON", e);
            }

            String routeJson = TemplateUtil.render(tpl.getRouteTemplate(), context);
            Map<String, Object> route;
            try {
                route = mapper.readValue(routeJson, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse route_template JSON", e);
            }

            String routeId = "autogen_" + api.getName() + "_" + userName;
            route.put("id", routeId);
            route.put("plugins", plugins);
            route.put("vars", vars);

            try {
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> routeEntity = new HttpEntity<>(mapper.writeValueAsString(route), headers);
                restTemplate.put("http://localhost:9180/apisix/admin/routes/" + routeId, routeEntity);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            ApiBinding record = apiBindingRepo
                .findByUserNameAndPersonaTypeAndApiId(userName, personaType, api.getId())
                .orElse(new ApiBinding());

            record.setUserName(userName);
            record.setPersonaType(personaType);
            record.setApiId(api.getId());
            record.setBoundAt(LocalDateTime.now());
            try {
                record.setBoundVars(mapper.writeValueAsString(vars));
                record.setTemplateContext(mapper.writeValueAsString(context));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize binding record", e);
            }

            apiBindingRepo.save(record);
        }
    }
}