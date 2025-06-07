package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_bindings")
@Data
public class ApiBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String personaType;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "upstream_id")
    private String upstreamId;

    @Column(columnDefinition = "TEXT")
    private String boundVars;

    @Column(columnDefinition = "TEXT")
    private String templateContext;

    private LocalDateTime boundAt = LocalDateTime.now();

    @Column(name = "api_id")
    private Long apiId;
}
