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

            Map<String, Object> context = prepareContext(api, userName, personaType, apiKey, extraParams);
            validateTemplates(tpl, upstreamTpl, context);

            boolean useMulti = false;
            if (extraParams != null) {
                Object mu = extraParams.get("multi_upstreams");
                useMulti = mu instanceof List && !((List<?>) mu).isEmpty();
            }

            List<String> upstreamIds = new ArrayList<>();
            String upstreamIdFromTpl = null;
            if (!useMulti) {
                upstreamIdFromTpl = createUpstream(upstreamTpl, context);
                upstreamIds.add(upstreamIdFromTpl);
            }

            List<List<String>> vars = templateRenderer.renderVars(tpl.getVarsTemplate(), personaType, context);
            Map<String, Object> plugins = templateRenderer.renderPlugins(tpl.getPluginTemplate(), context);

            processMultipleUpstreamPlugin(plugins, context, upstreamIds, upstreamTpl);

            if (useMulti && !upstreamIds.isEmpty()) {
                context.put("upstream_id", upstreamIds.get(0));
            }

            Map<String, Object> route = buildRoute(tpl, plugins, vars, context);
            String routeId = (String) route.get("id");
            routeManager.createOrUpdateRoute(routeId, route);

            saveSubscriptionRecord(api, userName, personaType, routeId, upstreamIds, vars, context);
        }
    }

    private Map<String, Object> prepareContext(ApiDefinition api, String userName, String personaType, String apiKey, Map<String, Object> extraParams) {
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
                    throw new IllegalArgumentException("limit-count plugin requires both count and time_window to be set.");
                }
            }
        }

        context.put("limit_count_plugin", limitCountPlugin);
        return context;
    }

    private void validateTemplates(RouteTemplate tpl, UpstreamTemplate upstreamTpl, Map<String, Object> context) {
        TemplateValidator.validateTemplateVariables(
                tpl.getRouteTemplate(), context,
                "route_template", Set.of("upstream_id"));
        TemplateValidator.validateTemplateVariables(
                tpl.getPluginTemplate(), context,
                "plugin_template");
        TemplateValidator.validateTemplateVariables(
                tpl.getVarsTemplate(), context,
                "vars_template");
        Object mu = context.get("multi_upstreams");
        boolean useMulti = mu instanceof List && !((List<?>) mu).isEmpty();

        if (!useMulti) {
            TemplateValidator.validateTemplateVariables(
                    upstreamTpl.getUpstreamTemplate(), context,
                    "upstream_template", Set.of("upstream_id"));
            TemplateValidator.validateIfNodeTemplateUsed(upstreamTpl.getUpstreamTemplate(), context);
        } else {
            for (Object obj : (List<?>) mu) {
                if (!(obj instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> itemCtx = new java.util.HashMap<>(context);
                itemCtx.putAll((Map<String, Object>) obj);
                TemplateValidator.validateTemplateVariables(
                        upstreamTpl.getUpstreamTemplate(), itemCtx,
                        "upstream_template", Set.of("upstream_id", "nodes_json"));
                TemplateValidator.validateIfNodeTemplateUsed(upstreamTpl.getUpstreamTemplate(), itemCtx);
            }
        }
    }

    private String createUpstream(UpstreamTemplate upstreamTpl, Map<String, Object> context) {
        Map<String, Object> desiredUpstream =
            templateRenderer.renderUpstream(upstreamTpl.getUpstreamTemplate(), context);

        Object idObj = desiredUpstream.get("id");
        String upstreamIdFromTpl =
            (idObj instanceof String && !((String) idObj).isBlank())
                ? (String) idObj
                : IdUtil.generateId("u-", desiredUpstream, mapper);

        desiredUpstream.put("id", upstreamIdFromTpl);
        context.put("upstream_id", upstreamIdFromTpl);

        upstreamManager.ensureUpstream(desiredUpstream);
        return upstreamIdFromTpl;
    }

    @SuppressWarnings("unchecked")
    private void processMultipleUpstreamPlugin(Map<String, Object> plugins, Map<String, Object> context,
                                              List<String> upstreamIds, UpstreamTemplate baseUpstreamTpl) {
        if (!plugins.containsKey("multiple-upstream-plugin")) {
            return;
        }

        Map<String, Object> pluginConf = (Map<String, Object>) plugins.get("multiple-upstream-plugin");
        Object rulesObj = pluginConf.get("rules");
        List<Object> rules = new ArrayList<>();

        if (rulesObj instanceof List<?>) {
            rules.addAll((List<Object>) rulesObj);
        }

        Object multiObj = context.get("multi_upstreams");
        if (multiObj instanceof List<?> multiList && baseUpstreamTpl != null) {
            for (Object itemObj : multiList) {
                if (!(itemObj instanceof Map)) {
                    continue;
                }
                Map<String, Object> item = new HashMap<>((Map<String, Object>) itemObj);

                Map<String, Object> ruleContext = new HashMap<>(context);
                ruleContext.putAll(item);

                Object itemNodes = ruleContext.get("nodes");
                if (itemNodes instanceof List<?> list && !list.isEmpty()) {
                    try {
                        ruleContext.put("nodes_json", mapper.writeValueAsString(itemNodes));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize nodes", e);
                    }
                }

                Map<String, Object> up = templateRenderer.renderUpstream(baseUpstreamTpl.getUpstreamTemplate(), ruleContext);
                String dynamicUid = IdUtil.generateId("u-", up, mapper);
                up.put("id", dynamicUid);
                ruleContext.put("upstream_id", dynamicUid);
                upstreamManager.ensureUpstream(up);
                upstreamIds.add(dynamicUid);

                Map<String, Object> newRule = new LinkedHashMap<>();

                if (item.containsKey("match")) {
                    Map<String, Object> match = new LinkedHashMap<>();
                    List<Map<String, Object>> vars = new ArrayList<>();
                    Map<String, Object> cond = new LinkedHashMap<>();
                    cond.put("var_name", "http_x_model");
                    cond.put("operator", "==");
                    cond.put("value", String.valueOf(item.get("match")));
                    vars.add(cond);
                    match.put("vars", vars);
                    newRule.put("match", match);
                }

                newRule.put("upstream_id", dynamicUid);

                if (item.containsKey("api_key")) {
                    List<Map<String, Object>> headers = new ArrayList<>();
                    Map<String, Object> header = new LinkedHashMap<>();
                    header.put("key", "api-key");
                    header.put("value", String.valueOf(item.get("api_key")));
                    headers.add(header);
                    newRule.put("headers", headers);
                }

                rules.add(newRule);
            }
        } else if (rulesObj instanceof List<?>) {
            // fallback to legacy behaviour where rules specify upstream template codes
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
                    rules.add(rule);
                }
            }
        }

        pluginConf.put("rules", rules);
    }

    private Map<String, Object> buildRoute(RouteTemplate tpl, Map<String, Object> plugins, List<List<String>> vars, Map<String, Object> context) {
        Map<String, Object> route = templateRenderer.renderRoute(tpl.getRouteTemplate(), context);
        route.put("plugins", plugins);
        route.put("vars", vars);
        String routeId = IdUtil.generateId("r-", route, mapper);
        route.put("id", routeId);
        return route;
    }

    private void saveSubscriptionRecord(ApiDefinition api, String userName, String personaType, String routeId,
                                        List<String> upstreamIds, List<List<String>> vars, Map<String, Object> context) {
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
