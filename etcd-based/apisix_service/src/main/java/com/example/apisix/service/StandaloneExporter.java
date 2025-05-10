package com.example.apisix.service;

import com.example.apisix.entity.*;
import com.example.apisix.repository.*;
import com.example.apisix.utils.TemplateUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.Writer;
import java.util.*;

@Service
@AllArgsConstructor
public class StandaloneExporter {
    private final ApiBindingRepository bindingRepo;
    private final ApiRepository apiRepo;
    private final TemplateRepository tplRepo;
    private final ObjectMapper mapper;

    public void exportAllToYaml() throws Exception {
        List<ApiBinding> bindings = bindingRepo.findAll();
        List<Map<String, Object>> routes = new ArrayList<>();

        for (ApiBinding binding : bindings) {
            ApiDefinition api = apiRepo.findByName(binding.getApiName());
            if (api == null) {
                throw new RuntimeException("API not found: " + binding.getApiName());
            }

            RouteTemplate tpl = tplRepo.findByCode(api.getRouteTemplateCode());
            if (tpl == null) {
                throw new RuntimeException("RouteTemplate not found: " + api.getRouteTemplateCode());
            }

            Map<String, Object> context = mapper.readValue(
                binding.getTemplateContext(), new TypeReference<>() {}
            );
            context.put("uri", api.getUri());
            context.put("name", api.getName());
            context.put("serviceName", api.getServiceName());
            context.put("upstream_id", api.getName() + "-upstream");

            // 渲染 vars
            String renderedVars = TemplateUtil.render(tpl.getVarsTemplate(), context);
            Map<String, List<List<String>>> varsMap = mapper.readValue(renderedVars, new TypeReference<>() {});
            List<List<String>> vars = varsMap.getOrDefault(binding.getPersonaType(), new ArrayList<>());

            // 渲染 plugins
            String pluginJson = TemplateUtil.render(tpl.getPluginTemplate(), context);
            Map<String, Object> plugins = mapper.readValue(pluginJson, new TypeReference<>() {});

            // 渲染 route
            String routeJson = TemplateUtil.render(tpl.getRouteTemplate(), context);
            Map<String, Object> route = mapper.readValue(routeJson, new TypeReference<>() {});

            String routeId = "route_" + api.getName() + "_" + binding.getUserName();
            route.put("id", routeId);
            route.put("plugins", plugins);
            route.put("vars", vars);

            routes.add(route);
        }

        Map<String, Object> yamlMap = Map.of("routes", routes);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        try (Writer writer = new FileWriter("./apisix.yaml")) {
            yaml.dump(yamlMap, writer);
        }

        // 執行 reload
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", "apisix reload");
        pb.inheritIO().start().waitFor();
    }
}
