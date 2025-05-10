package com.example.apisix.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BindRequest {
    private String userName;
    private String personaType;
    private String apiKey;
    private List<String> apis;

    public String getUserName() { return userName; }
    public String getPersonaType() { return personaType; }
    public String getApiKey() { return apiKey; }
    public List<String> getApis() { return apis; }
}
