package com.example.apisix.service;

import com.example.apisix.dto.BindRequest;
import reactor.core.publisher.Mono;

public interface BindService {
    Mono<Void> bind(BindRequest request);
}
