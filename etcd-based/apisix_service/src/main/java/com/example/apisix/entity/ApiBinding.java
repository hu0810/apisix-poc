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
    private String apiName;

    @Column(columnDefinition = "TEXT")
    private String boundVars;

    @Column(columnDefinition = "TEXT")
    private String templateContext;

    private LocalDateTime boundAt = LocalDateTime.now();
}