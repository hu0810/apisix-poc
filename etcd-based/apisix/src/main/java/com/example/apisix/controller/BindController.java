package com.example.apisix.controller;

import com.example.apisix.dto.BindRequest;
import com.example.apisix.service.BindService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/bind")
@RequiredArgsConstructor
public class BindController {

    private final BindService bindService;

    @PostMapping
    public Mono<Void> bind(@Valid @RequestBody BindRequest request) {
        return bindService.bind(request);
    }
}
