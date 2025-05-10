package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "persona_var_mappings")
@Data
public class PersonaVarMapping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String personaType;
    private String routeTemplateCode;
    @Column(columnDefinition = "TEXT")
    private String varTemplate;
}