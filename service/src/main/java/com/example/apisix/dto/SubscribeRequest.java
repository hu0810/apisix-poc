package com.example.apisix.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SubscribeRequest {
    private String userName;
    private String personaType;
    private String apiKey;
    private List<String> apis;
    private Map<String, Object> extraParams;

}
