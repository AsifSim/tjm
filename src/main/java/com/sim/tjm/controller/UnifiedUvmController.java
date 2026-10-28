package com.sim.tjm.controller;

import com.sim.tjm.model.ApiEndpoint;
import com.sim.tjm.model.AppConfiguration;
import com.sim.tjm.repo.AppConfigurationRepository;
import com.sim.tjm.service.DynamicApiManager; // Import the manager
import com.sim.tjm.service.UnifiedUvmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/uvm")
public class UnifiedUvmController {

    @Autowired
    private AppConfigurationRepository repository;

    @Autowired
    private UnifiedUvmService unifiedUvmService;

    // Inject the DynamicApiManager
    @Autowired
    private DynamicApiManager dynamicApiManager;

    @GetMapping("/entities")
    public ResponseEntity<List<Map<String, Object>>> getEntities() {
        return ResponseEntity.ok(unifiedUvmService.getAvailableEntities());
    }

    @PostMapping("/execute/{appId}/{apiRef}")
    public ResponseEntity<Map<String, Object>> executeUvmProtocol(
            @PathVariable String appId,
            @PathVariable String apiRef,
            @RequestParam(required = false) String source,
            @RequestBody(required = false) Map<String, Object> payload) {

        AppConfiguration config = repository.findById(appId)
                .orElseThrow(() -> new RuntimeException("App ID not found: " + appId));

        ApiEndpoint targetApi = config.getApis().get(apiRef);
        if (targetApi == null) throw new RuntimeException("API Reference not found: " + apiRef);

        String defaultTrigger = (targetApi.getTjmSequence() != null && targetApi.getTjmSequence().getSourceTrigger() != null)
                ? targetApi.getTjmSequence().getSourceTrigger() : "CONSOLE";
        String executionSource = (source != null && !source.isEmpty()) ? source : defaultTrigger;

        Map<String, Object> tjmResult = unifiedUvmService.executeTjmSequence(appId, targetApi, payload, executionSource);

        Map<String, Object> protocolResponse = new LinkedHashMap<>();
        protocolResponse.put("mode", executionSource);
        protocolResponse.put("status", "SUCCESS");
        protocolResponse.put("data", tjmResult);
        protocolResponse.put("trace_id", UUID.randomUUID().toString());
        protocolResponse.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(protocolResponse);
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveConfiguration(@RequestBody AppConfiguration config) {
        // 1. Save to PostgreSQL
        AppConfiguration saved = repository.save(config);

        // 2. Hot-reload the endpoint into the live Spring Context
        if (saved.getApis() != null) {
            saved.getApis().forEach((apiRef, api) -> {
                dynamicApiManager.registerEndpoint(api.getApiUrl(), api.getMethod(), saved.getAppId(), apiRef);
            });
        }

        return ResponseEntity.ok(saved);
    }
}