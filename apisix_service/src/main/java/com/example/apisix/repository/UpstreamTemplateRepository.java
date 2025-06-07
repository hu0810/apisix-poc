package com.example.apisix.repository;

import com.example.apisix.entity.UpstreamTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UpstreamTemplateRepository extends JpaRepository<UpstreamTemplate, Integer> {
    UpstreamTemplate findByCode(String code);
}