package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "api_definitions")
@Data
public class ApiDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String uri;
    private String serviceName;
    private String routeTemplateCode;
}