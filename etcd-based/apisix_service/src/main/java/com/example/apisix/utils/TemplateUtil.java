package com.example.apisix.utils;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.io.UncheckedIOException;
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
}