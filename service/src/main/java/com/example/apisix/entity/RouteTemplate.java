package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "route_templates")
@Data
public class RouteTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String description;

    @Column(name = "route_template", columnDefinition = "TEXT")
    private String routeTemplate;

    @Column(name = "plugin_template", columnDefinition = "TEXT")
    private String pluginTemplate;

    @Column(name = "vars_template", columnDefinition = "TEXT")
    private String varsTemplate;
}
