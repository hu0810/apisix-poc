package com.example.apisix.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.apisix.utils.TemplateUtil;

public class IdUtil {
    /**
     * Generate an ID with the given prefix based on the short hash of the provided object.
     * The object will be serialized to JSON using the supplied ObjectMapper.
     */
    public static String generateId(String prefix, Object obj, ObjectMapper mapper) {
        try {
            String json = (obj instanceof String) ? (String) obj : mapper.writeValueAsString(obj);
            return prefix + TemplateUtil.shortHash(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to generate id", e);
        }
    }
}
