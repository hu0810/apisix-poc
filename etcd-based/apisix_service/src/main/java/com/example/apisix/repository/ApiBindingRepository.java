package com.example.apisix.repository;

import com.example.apisix.entity.ApiBinding;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiBindingRepository extends JpaRepository<ApiBinding, Long> {
    Optional<ApiBinding> findByUserNameAndPersonaTypeAndApiId(String userName, String personaType, Long apiId);
}