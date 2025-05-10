package com.example.apisix.repository;

import com.example.apisix.entity.ApiBinding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiBindingRepository extends JpaRepository<ApiBinding, Long> {
}