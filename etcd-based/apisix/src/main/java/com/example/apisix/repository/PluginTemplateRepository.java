package com.example.apisix.repository;

import com.example.apisix.entity.PluginTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginTemplateRepository extends JpaRepository<PluginTemplate, Long> {
}
