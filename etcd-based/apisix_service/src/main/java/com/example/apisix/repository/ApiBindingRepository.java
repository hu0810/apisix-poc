package com.example.apisix.repository;

import com.example.apisix.entity.ApiBinding;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiBindingRepository extends JpaRepository<ApiBinding, Long> {
    Optional<ApiBinding> findByUserNameAndPersonaTypeAndApiName(String userName, String personaType, String apiName);
}