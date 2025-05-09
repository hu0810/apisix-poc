package com.example.apisix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BindRequest {

    @NotBlank
    private String user;

    @NotBlank
    private String personaType;

    @NotEmpty
    private List<Long> apiIds;
}
