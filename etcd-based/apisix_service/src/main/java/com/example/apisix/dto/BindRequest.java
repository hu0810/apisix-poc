package com.example.apisix.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BindRequest {
    private String userName;
    private String personaType;
    private String apiKey;
    private List<String> apis;
    private Map<String, String> extraParams; 

    public String getUserName() { return userName; }
    public String getPersonaType() { return personaType; }
    public String getApiKey() { return apiKey; }
    public List<String> getApis() { return apis; }
    public Map<String, String> getExtraParams() { return extraParams; }

}
