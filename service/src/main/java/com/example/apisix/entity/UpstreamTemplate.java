package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "upstream_templates")
public class UpstreamTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;

    @Column(length = 500)
    private String description;

    @Column(name = "upstream_template", columnDefinition = "TEXT")
    private String upstreamTemplate;
}
