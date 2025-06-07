package com.example.apisix.repository;

import com.example.apisix.entity.ApiSubscription;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiSubscriptionRepository extends JpaRepository<ApiSubscription, Long> {
    Optional<ApiSubscription> findByUserNameAndPersonaTypeAndApiId(String userName, String personaType, Long apiId);
}
