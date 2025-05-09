package com.example.apisix.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "api_variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"api_id", "var_key"})
})
public class ApiVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_id", nullable = false)
    private Long apiId;

    @Column(name = "var_key", nullable = false)
    private String varKey;

    @Column(name = "var_value", nullable = false, length = 1024)
    private String varValue;
}
