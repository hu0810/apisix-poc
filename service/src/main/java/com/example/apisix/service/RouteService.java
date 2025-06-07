package com.example.apisix.service;

import com.example.apisix.entity.*;
import com.example.apisix.repository.*;
import com.example.apisix.utils.TemplateValidator;
import com.example.apisix.utils.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RouteService {
    private final ApiRepository apiRepo;
    private final TemplateRepository tplRepo;
    private final UpstreamTemplateRepository upstreamTplRepo;
    private final ApiSubscriptionRepository apiSubscriptionRepo;
    private final ObjectMapper mapper;
    private final TemplateRenderer templateRenderer;
    private final RouteManager routeManager;
    private final UpstreamManager upstreamManager;

    public void subscribeSmart(String userName, String personaType, String apiKey, List<String> apis, Map<String, Object> extraParams) {
        for (String apiName : apis) {
            ApiDefinition api = apiRepo.findByName(apiName);

            if (api == null) {
                throw new RuntimeException("API not found: " + apiName);
            }

            RouteTemplate tpl = tplRepo.findByCode(api.getRouteTemplateCode());
            UpstreamTemplate upstreamTpl = upstreamTplRepo.findByCode(api.getUpstreamTemplateCode());

            if (tpl == null || upstreamTpl == null) {
                throw new RuntimeException("Template not found for API: " + apiName);
            }

            Map<String, Object> context = new HashMap<>();
            context.put("userName", userName);
            context.put("personaType", personaType);
            context.put("apiKey", apiKey);
            context.put("uri", api.getUri());
            context.put("name", api.getName());
            context.put("serviceName", api.getServiceName());

            String limitCountPlugin = "";
            if (extraParams != null) {
                context.putAll(extraParams);

                Object rawNodes = extraParams.get("nodes");
                if (rawNodes instanceof List && !((List<?>) rawNodes).isEmpty()) {
                    context.put("nodes", rawNodes);
                    try {
                        context.put("nodes_json", mapper.writeValueAsString(rawNodes));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize nodes", e);
                    }
                }

                Object countObj = extraParams.get("count");
                Object timeWindowObj = extraParams.get("time_window");
                if (countObj != null || timeWindowObj != null) {
                    if (countObj != null && timeWindowObj != null) {
                        limitCountPlugin = ", \"limit-count\": {" +
                                "\"count\": " + countObj + "," +
                                "\"time_window\": " + timeWindowObj + "," +
                                "\"key\": \"remote_addr\"," +
                                "\"policy\": \"local\"}";
                    } else {
                        throw new IllegalArgumentException(
                                "limit-count plugin requires both count and time_window to be set.");
                    }
                }
            }

            context.put("limit_count_plugin", limitCountPlugin);

            Map<String, String> basicTemplates = Map.of(
                "route_template", tpl.getRouteTemplate(),
                "plugin_template", tpl.getPluginTemplate(),
                "vars_template", tpl.getVarsTemplate()
            );
            TemplateValidator.validateAllTemplates(basicTemplates, context);
            TemplateValidator.validateTemplateVariables(
                    upstreamTpl.getUpstreamTemplate(), context,
                    "upstream_template", Set.of("upstream_id"));
            TemplateValidator.validateIfNodeTemplateUsed(upstreamTpl.getUpstreamTemplate(), context);

            Map<String, Object> desiredUpstream = templateRenderer.renderUpstream(upstreamTpl.getUpstreamTemplate(), context);
            String upstreamIdFromTpl = IdUtil.generateId("u-", desiredUpstream, mapper);
            desiredUpstream.put("id", upstreamIdFromTpl);
            context.put("upstream_id", upstreamIdFromTpl);
            upstreamManager.ensureUpstream(desiredUpstream);

            List<String> upstreamIds = new ArrayList<>();
            upstreamIds.add(upstreamIdFromTpl);

            List<List<String>> vars = templateRenderer.renderVars(tpl.getVarsTemplate(), personaType, context);
            Map<String, Object> plugins = templateRenderer.renderPlugins(tpl.getPluginTemplate(), context);

            if (plugins.containsKey("multiple-upstream-plugin")) {
                Object rulesObj = ((Map<String, Object>) plugins.get("multiple-upstream-plugin")).get("rules");
                if (rulesObj instanceof List<?>) {
                    for (Object r : (List<?>) rulesObj) {
                        if (r instanceof Map) {
                            Map<String, Object> rule = (Map<String, Object>) r;
                            String uid = (String) rule.get("upstream_id");
                            if (uid != null) {
                                UpstreamTemplate extraTpl = upstreamTplRepo.findByCode(uid);
                                if (extraTpl != null) {
                                    Map<String, Object> ruleContext = new HashMap<>(context);
                                    Map<String, Object> up = templateRenderer.renderUpstream(extraTpl.getUpstreamTemplate(), ruleContext);
                                    String dynamicUid = IdUtil.generateId("u-", up, mapper);
                                    ruleContext.put("upstream_id", dynamicUid);
                                    up.put("id", dynamicUid);
                                    upstreamManager.ensureUpstream(up);
                                    rule.put("upstream_id", dynamicUid);
                                    upstreamIds.add(dynamicUid);
                                } else {
                                    upstreamIds.add(uid);
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> route = templateRenderer.renderRoute(tpl.getRouteTemplate(), context);

            route.put("plugins", plugins);
            route.put("vars", vars);
            String routeId = IdUtil.generateId("r-", route, mapper);
            route.put("id", routeId);

            routeManager.createOrUpdateRoute(routeId, route);

            ApiSubscription record = apiSubscriptionRepo
                .findByUserNameAndPersonaTypeAndApiId(userName, personaType, api.getId())
                .orElse(new ApiSubscription());

            record.setUserName(userName);
            record.setPersonaType(personaType);
            record.setRouteId(routeId);
            try {
                record.setUpstreamIds(mapper.writeValueAsString(upstreamIds));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize upstreamIds", e);
            }
            record.setApiId(api.getId());
            record.setSubscribedAt(LocalDateTime.now());
            try {
                record.setSubscribedVars(mapper.writeValueAsString(vars));
                record.setTemplateContext(mapper.writeValueAsString(context));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize subscription record", e);
            }

            apiSubscriptionRepo.save(record);
        }
    }
}