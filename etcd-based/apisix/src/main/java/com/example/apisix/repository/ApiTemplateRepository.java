package com.example.apisix.repository;

import com.example.apisix.entity.ApiTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTemplateRepository extends JpaRepository<ApiTemplate, Long> {
}
