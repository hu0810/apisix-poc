package com.example.apisix.repository;

import com.example.apisix.entity.RouteTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<RouteTemplate, Long> {
    RouteTemplate findByCode(String code);
}