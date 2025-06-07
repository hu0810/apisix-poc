package com.example.apisix.utils;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class TemplateUtil {

    private static final PebbleEngine ENGINE = new PebbleEngine.Builder()
            .autoEscaping(false)   // 禁止 HTML escape（否則冒號會變成 &#58;）
            .newLineTrimming(true) // 移除模板中的換行符
            .build();

    public static String render(String templateContent, Map<String, Object> context) {
        try {
            // 直接用字串當模板，不用再包一個 StringReader
            PebbleTemplate template = ENGINE.getLiteralTemplate(templateContent);
            StringWriter writer = new StringWriter();
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw (e instanceof java.io.IOException)
                    ? new UncheckedIOException((java.io.IOException) e)
                    : new RuntimeException("Template rendering failed", e);
        }
    }

    /**
     * Generate an 8-character hash based on the given input string using SHA-256.
     */
    public static String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) { // first 4 bytes -> 8 hex chars
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }
}