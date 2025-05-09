package com.example.apisix.service;

import com.example.apisix.dto.BindRequest;
import com.example.apisix.entity.ApiTemplate;
import com.example.apisix.repository.ApiTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class BindServiceImpl implements BindService {

    private final ApiTemplateRepository apiTemplateRepository;
    private final WebClient apisixWebClient;

    @Override
    public Mono<Void> bind(BindRequest request) {

        return Flux.fromIterable(request.getApiIds())
                // 1) 把阻塞式 JPA 查詢包進 Mono.fromCallable
                .flatMap(id -> Mono.fromCallable(() -> apiTemplateRepository.findById(id))
                                   .subscribeOn(Schedulers.boundedElastic())   // 放到阻塞排程器
                                   .flatMap(Mono::justOrEmpty))               // Optional → Mono
                // 2) 呼叫 APISIX Admin API
                .flatMap(this::callAdminApi)
                .then();
    }

    private Mono<Void> callAdminApi(ApiTemplate tpl) {
        return apisixWebClient
                .put()
                .uri("/routes/{id}", tpl.getId())
                .bodyValue(tpl.getJsonTemplate())
                .retrieve()
                .bodyToMono(String.class)
                .then();
    }
}
