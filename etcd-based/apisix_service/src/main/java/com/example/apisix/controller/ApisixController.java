package com.example.apisix.controller;

import com.example.apisix.dto.BindRequest;
import com.example.apisix.service.RouteService;
import com.example.apisix.service.StandaloneExporter;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/apisix")
@AllArgsConstructor
public class ApisixController {
    private final RouteService routeService;

    @PostMapping("/bind")
    public ResponseEntity<?> bind(@RequestBody BindRequest req) {
    
        try {
            routeService.bindSmart(
                req.getUserName(),
                req.getPersonaType(),
                req.getApiKey(),
                req.getApis(),
                req.getExtraParams()
            );
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity
                .status(500)
                .body("Error: " + e.getMessage()); // ✅ 用 getMessage()
        }
    }

    private final StandaloneExporter exporter;

    @PostMapping("/export")
    public ResponseEntity<String> exportStandalone() {
        try {
            exporter.exportAllToYaml();
            return ResponseEntity.ok("Exported and reloaded");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed: " + e.getMessage());
        }
    }
}
