package com.example.apisix.repository;

import com.example.apisix.entity.ApiVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiVariableRepository extends JpaRepository<ApiVariable, Long> {
    List<ApiVariable> findByApiId(Long apiId);
}
