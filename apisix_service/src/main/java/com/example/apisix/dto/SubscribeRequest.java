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

    public String getUserName() { return userName; }
    public String getPersonaType() { return personaType; }
    public String getApiKey() { return apiKey; }
    public List<String> getApis() { return apis; }
    public Map<String, Object> getExtraParams() { return extraParams; }

}
