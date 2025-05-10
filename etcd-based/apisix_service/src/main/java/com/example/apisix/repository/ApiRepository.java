package com.example.apisix.repository;

import com.example.apisix.entity.ApiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiRepository extends JpaRepository<ApiDefinition, Long> {
    ApiDefinition findByName(String name);
}
