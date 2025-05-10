package com.example.apisix.controller;

import com.example.apisix.dto.BindRequest;
import com.example.apisix.service.RouteService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/apisix")
@AllArgsConstructor
public class ApisixController {
    private final RouteService routeService;

    @PostMapping("/bind")
    public String bindUserToApis(@RequestBody BindRequest req) {
        routeService.bindUserToApis(req.getUserName(), req.getPersonaType(), req.getApiKey(), req.getApis());
        return "OK";
    }
}
