package com.example.apisix.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TemplateValidator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
    private static final Pattern IF_BLOCK_PATTERN = Pattern.compile("\\{\\%\\s*if.*?\\%\\}(.*?)\\{\\%\\s*endif\\s*\\%\\}", Pattern.DOTALL);
    private static final Pattern IF_CONDITION_PATTERN = Pattern.compile("\\{\\%\\s*if\\s+(.*?)\\s*\\%\\}");

    /**
     * 抓出所有 {{ xxx }} 的變數
     */
    public static Set<String> extractAllVariables(String template) {
        Set<String> vars = new HashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }

    /**
     * 抓出所有在 {% if ... %} 區塊中的 {{ xxx }} 變數
     */
    public static Set<String> extractOptionalVariables(String template) {
        Set<String> optionalVars = new HashSet<>();

        // {% if ... %} ... {% endif %}
        Matcher blockMatcher = IF_BLOCK_PATTERN.matcher(template);
        while (blockMatcher.find()) {
            String block = blockMatcher.group(1);
            optionalVars.addAll(extractAllVariables(block));
        }

        // {% if count and time_window %} -> [count, time_window]
        Matcher condMatcher = IF_CONDITION_PATTERN.matcher(template);
        while (condMatcher.find()) {
            String condition = condMatcher.group(1);
            for (String token : condition.split("\\s*(and|or|\\(|\\))\\s*")) {
                if (!token.isBlank() && token.matches("\\w+")) {
                    optionalVars.add(token.trim());
                }
            }
        }

        return optionalVars;
    }

    /**
     * 驗證模板中所有必填變數是否有出現在 context 中
     */
    public static void validateTemplateVariables(String template, Map<String, Object> context, String templateName) {
        Set<String> allVars = extractAllVariables(template);
        Set<String> optionalVars = extractOptionalVariables(template);
        Set<String> requiredVars = new HashSet<>(allVars);
        requiredVars.removeAll(optionalVars);

        Set<String> missing = requiredVars.stream()
                .filter(key -> !context.containsKey(key))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new RuntimeException("Missing required variables in template [" + templateName + "]: " + missing);
        }
    }

    /**
     * 批次驗證多個模板（只看變數完整性）
     */
    public static void validateAllTemplates(Map<String, String> templates, Map<String, Object> context) {
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            validateTemplateVariables(entry.getValue(), context, entry.getKey());
        }
    }

    // ====== 進階內容驗證（nodes 結構） ======

    /**
     * 驗證 nodes 欄位是否為非空陣列，且每個 node 都有 host / port / weight
     */
    @SuppressWarnings("unchecked")
    public static void validateNodesStructure(Map<String, Object> context) {
        Object rawNodes = context.get("nodes");
        if (!(rawNodes instanceof List) || ((List<?>) rawNodes).isEmpty()) {
            throw new RuntimeException("`nodes` must be a non-empty list");
        }

        List<?> nodes = (List<?>) rawNodes;
        for (Object obj : nodes) {
            if (!(obj instanceof Map)) {
                throw new RuntimeException("Each item in `nodes` must be a Map");
            }
            Map<String, Object> node = (Map<String, Object>) obj;
            if (!node.containsKey("host") || !node.containsKey("port") || !node.containsKey("weight")) {
                throw new RuntimeException("Each node must have `host`, `port`, and `weight`");
            }
        }
    }

    /**
     * 根據 upstream template 是否需要 nodes 參數來決定是否驗證 nodes 結構
     */
    public static void validateIfNodeTemplateUsed(String upstreamTemplate, Map<String, Object> context) {
        if (upstreamTemplate.contains("{{ node.") || upstreamTemplate.contains("{{nodes_json}}")) {
            validateNodesStructure(context);
        }
    }
}
