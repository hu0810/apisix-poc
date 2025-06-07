package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_subscriptions")
@Data
public class ApiSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userName;
    private String personaType;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "upstream_ids", columnDefinition = "TEXT")
    private String upstreamIds;

    @Column(columnDefinition = "TEXT")
    private String subscribedVars;

    @Column(columnDefinition = "TEXT")
    private String templateContext;

    private LocalDateTime subscribedAt = LocalDateTime.now();

    @Column(name = "api_id")
    private Long apiId;
}
