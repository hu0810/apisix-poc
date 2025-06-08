package com.example.apisix.service;

import com.example.apisix.utils.TemplateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TemplateRenderer {
    private final ObjectMapper mapper;

    public TemplateRenderer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> renderUpstream(String upstreamTemplate, Map<String, Object> context) {
        String rendered = TemplateUtil.render(upstreamTemplate, context);
        try {
            return mapper.readValue(rendered, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse rendered upstream_template JSON", e);
        }
    }

    public List<List<String>> renderVars(String varsTemplate, String personaType, Map<String, Object> context) {
        String rendered = TemplateUtil.render(varsTemplate, context);
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
        return vars;
    }

    public Map<String, Object> renderPlugins(String pluginTemplate, Map<String, Object> context) {
        try {
            String pluginJson = TemplateUtil.render(pluginTemplate, context);
            Map<String, Object> plugins = mapper.readValue(pluginJson, new TypeReference<>() {});
            if (plugins.containsKey("__template_warning__")) {
                throw new IllegalArgumentException((String) plugins.get("__template_warning__"));
            }
            return plugins;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse plugin_template JSON", e);
        }
    }

    public Map<String, Object> renderRoute(String routeTemplate, Map<String, Object> context) {
        String routeJson = TemplateUtil.render(routeTemplate, context);
        try {
            return mapper.readValue(routeJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse route_template JSON", e);
        }
    }

    /**
     * Render the route template and inject rendered plugins and vars.
     * <p>
     * This is a convenience method that combines the functionality of
     * {@link #renderVars(String, String, Map)} and
     * {@link #renderPlugins(String, Map)} when building a route object.
     */
    public Map<String, Object> renderRoute(
            String routeTemplate,
            String pluginTemplate,
            String varsTemplate,
            String personaType,
            Map<String, Object> context) {
        Map<String, Object> route = renderRoute(routeTemplate, context);
        Map<String, Object> plugins = renderPlugins(pluginTemplate, context);
        List<List<String>> vars = renderVars(varsTemplate, personaType, context);
        route.put("plugins", plugins);
        route.put("vars", vars);
        return route;
    }
}
