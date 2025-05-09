package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "persona_variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"persona_type", "var_key"})
})
public class PersonaVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "persona_type", nullable = false)
    private String personaType;

    @Column(name = "var_key", nullable = false)
    private String varKey;

    @Column(name = "var_value", nullable = false, length = 1024)
    private String varValue;
}
